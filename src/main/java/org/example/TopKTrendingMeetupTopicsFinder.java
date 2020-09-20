package org.example;

import com.google.common.io.Files;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Seconds;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.example.config.KafkaConsumerConfig;
import org.example.model.GroupTopics;
import org.example.model.MeetupRsvpsMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

@Builder
@Data
@Slf4j
public class TopKTrendingMeetupTopicsFinder {
    private static final String RUN_LOCAL_WITH_AVAILABLE_CORES = "spark://spark-master:7077";
    private static final String APPLICATION_NAME = "TopKTrendingMeetupTopicsFinder";
    private static final String CASE_SENSITIVE = "false";
    private static final int BATCH_DURATION_INTERVAL_MS = 10000;
    private static final String timeZoneId = "UTC";
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");

    private String kafkaBrokers;
    private String inputTopicName;
    private int windowDurationInMinutes;
    private int numberOfTrendingTopicsPerMinutes;
    private String countryToFilter;
    private String cityToFilterName;
    private String outputFilePath;
    private KafkaConsumerConfig kafkaConsumerConfig;

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 7) {
            System.err.println("Usage: TopKTrendingMeetupTopicsFinder <Path to twitter credential file> <Output file path>");
            System.exit(1);
        }
        TopKTrendingMeetupTopicsFinder topKTrendingMeetupTopicsFinder = TopKTrendingMeetupTopicsFinder.builder()
                .kafkaBrokers(args[0])
                .inputTopicName(args[1])
                .windowDurationInMinutes(Integer.parseInt(args[2]))
                .numberOfTrendingTopicsPerMinutes(Integer.parseInt(args[3]))
                .countryToFilter(args[4])
                .cityToFilterName(args[5])
                .outputFilePath(args[6])
                .kafkaConsumerConfig(new KafkaConsumerConfig(args[0]))
                .build();

        JavaStreamingContext streamingContext = topKTrendingMeetupTopicsFinder.getJavaStreamingContext();
        topKTrendingMeetupTopicsFinder.findMeetupTredingTopicsWithWindow(streamingContext);

        streamingContext.start();
        streamingContext.awaitTermination();
    }

    /**
     *
     * @return
     */
    private JavaStreamingContext getJavaStreamingContext() {
        SparkConf conf = new SparkConf()
                .setMaster(RUN_LOCAL_WITH_AVAILABLE_CORES)
                .setAppName(APPLICATION_NAME)
                .set("spark.sql.caseSensitive", CASE_SENSITIVE);

        return new JavaStreamingContext(conf, new Duration(BATCH_DURATION_INTERVAL_MS));
    }

    /**
     *
     * @param streamingContext
     */
    private void findMeetupTredingTopicsWithWindow(JavaStreamingContext streamingContext) {
        JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> meetupRawEvents =
                readMeetupRsvpsEvents(streamingContext, inputTopicName, kafkaConsumerConfig.getKafkaConsumerProperties());
        JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent =
                filterEventsByCountyAndCity(countryToFilter, cityToFilterName, meetupRawEvents);
        JavaPairDStream<Integer, String> countPerTopicsInDescOrder = findCountPerTopicsInDescOrderWithWindow(
                meetupFilteredEvent, windowDurationInMinutes);
        countPerTopicsInDescOrder.print();
        writeTopKTrendingTopicPerWindowInFile(countPerTopicsInDescOrder, numberOfTrendingTopicsPerMinutes,
                windowDurationInMinutes, outputFilePath);
    }

    /**
     *
     * @param countPerTopicsInDescOrder
     * @param k
     * @param windowDurationInMinutes
     * @param outputFilePath
     */
    private void writeTopKTrendingTopicPerWindowInFile(
            JavaPairDStream<Integer, String> countPerTopicsInDescOrder, int k, int windowDurationInMinutes, String outputFilePath) {
        countPerTopicsInDescOrder.foreachRDD(rdd -> {
            writeTopHashtagsToFile(
                    new File(outputFilePath), k, windowDurationInMinutes, rdd);
        });
    }

    /**
     *
     * @param meetupFilteredEvent
     * @param windowDurationInMinutes
     * @return
     */
    private JavaPairDStream<Integer, String> findCountPerTopicsInDescOrderWithWindow(
            JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent, int windowDurationInMinutes) {
        JavaPairDStream<String, Integer> meetupAggregatedEvent = findTotalOfEveryUrlKey(
            meetupFilteredEvent, windowDurationInMinutes);
        JavaPairDStream<Integer, String> swappedPair = meetupAggregatedEvent.mapToPair(x -> x.swap());
        return swappedPair.transformToPair(jPairRDD -> jPairRDD.sortByKey(false));
    }

    /**
     *
     * @param meetupFilteredEvent
     * @param windowDurationInMinutes
     * @return
     */
    private JavaPairDStream<String, Integer> findTotalOfEveryUrlKey(
            JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent, int windowDurationInMinutes) {
        JavaPairDStream<String, Integer> count = meetupFilteredEvent.flatMapToPair(
                meetupRsvpsMessage -> {
                    List<Tuple2<String, Integer>> urlkeyCountTuples = new ArrayList<>();
                    for (GroupTopics group_topic : meetupRsvpsMessage.getGroup().getGroup_topics()) {
                        urlkeyCountTuples.add(new Tuple2(group_topic.getUrlkey().toLowerCase(), 1));
                    }
                    return urlkeyCountTuples.iterator();
                });

        return count.reduceByKeyAndWindow(
                (integer, integer2) -> integer + integer2, Seconds.apply(windowDurationInMinutes * 60));
    }

    /**
     *
     * @param streamingContext
     * @param inputTopicName
     * @param kafkaConsumerProperties
     * @return
     */
    private JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> readMeetupRsvpsEvents(
        JavaStreamingContext streamingContext, String inputTopicName, Map<String, Object> kafkaConsumerProperties) {
        return KafkaUtils.createDirectStream(
                streamingContext, LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(Collections.unmodifiableList(Arrays.asList(inputTopicName)),
                        kafkaConsumerProperties));
    }

    /**
     *
     * @param countryName
     * @param cityName
     * @param meetupRawEvents
     * @return
     */
    private JavaDStream<MeetupRsvpsMessage> filterEventsByCountyAndCity(String countryName, String cityName,
       JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> meetupRawEvents) {
        return meetupRawEvents.map(
            v -> v.value()).filter(meetupRsvpsMessage -> {
            return !meetupRsvpsMessage.getGroup().getGroup_country().equalsIgnoreCase(countryName)
            && meetupRsvpsMessage.getGroup().getGroup_city().equalsIgnoreCase(cityToFilterName);
        });
    }

    /**
     *
     * @param file
     * @param numberOfTrendingTopicsPerMinutes
     * @param windowDurationInMinutes
     * @param trendTopicRdd
     * @throws IOException
     */
    private void writeTopHashtagsToFile(File file, int numberOfTrendingTopicsPerMinutes,
       int windowDurationInMinutes, JavaPairRDD<Integer, String> trendTopicRdd) throws IOException {
        DateTime currentTime = DateTime.now(DateTimeZone.forID(timeZoneId));
        String timeRange = dateTimeFormat.print(currentTime.minusSeconds(windowDurationInMinutes * 60))
                + " to " + dateTimeFormat.print(currentTime) + " (UTC Time)";
        Files.append("Top " + numberOfTrendingTopicsPerMinutes + " meetup topics captured from " + timeRange + ":\n",
                file, Charset.forName("UTF-8"));

        List<Tuple2<Integer, String>> topTrendTopics = trendTopicRdd.take(numberOfTrendingTopicsPerMinutes);
        trendTopicRdd.foreach(rdd -> {
            try {
                Files.append(rdd.toString() + "\n", file, Charset.forName("UTF-8"));
            } catch (IOException e) {
                log.error("error while writing output to file");
            }
        });
    }
}