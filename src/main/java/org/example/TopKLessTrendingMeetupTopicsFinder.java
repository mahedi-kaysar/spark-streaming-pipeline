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
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
TopKLessTrendingMeetupTopicsFinder finds top K less trending topics every M minutes from meetup-rsvps streams.
The meetup-rsvps streams are in json format. Firstly KafkaConsumer deserialize into Java Objects and
then objects are filtered by given country and city and then finds the total count of each topic (field name: urlkey)
in ascending order in a given M minutes time window. Finally the first K topics are saved into a file.
 */
@Builder
@Data
@Slf4j
public class TopKLessTrendingMeetupTopicsFinder implements Serializable {
    private static final String RUN_LOCAL_WITH_AVAILABLE_CORES = "spark://spark-master:7077";
    private static final String APPLICATION_NAME = "TopKLessTrendingMeetupTopicsFinder";
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
     * The args take mandetory 7 arguments and 6th argument (city) can be put as empty
     * string ("") to exclude city to be filtered.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 7) {
            System.err.println("Usage: TopKLessTrendingMeetupTopicsFinder <kafkaBrokers> <inputTopicName> "
                + "<windowDurationInMinutes> <numberOfTrendingTopicsPerMinutes> <countryToFilter> "
                + "<cityToFilterName> <outputFilePath>");
            System.exit(1);
        }

        TopKLessTrendingMeetupTopicsFinder topKLessTrendingMeetupTopicsFinder = TopKLessTrendingMeetupTopicsFinder.builder()
            .kafkaBrokers(args[0])
            .inputTopicName(args[1])
            .windowDurationInMinutes(Integer.parseInt(args[2]))
            .numberOfTrendingTopicsPerMinutes(Integer.parseInt(args[3]))
            .countryToFilter(args[4])
            .cityToFilterName(args[5])
            .outputFilePath(args[6])
            .kafkaConsumerConfig(new KafkaConsumerConfig(args[0]))
            .build();

        JavaStreamingContext streamingContext = topKLessTrendingMeetupTopicsFinder.initializeJavaStreamingContext();
        topKLessTrendingMeetupTopicsFinder.findAndSaveMeetupTrendingTopicsWithWindow(streamingContext);

        streamingContext.start();
        streamingContext.awaitTermination();
    }

    /**
     * This method initialize spark streaming context.
     *
     * @return
     */
    private JavaStreamingContext initializeJavaStreamingContext() {
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
    private void findAndSaveMeetupTrendingTopicsWithWindow(JavaStreamingContext streamingContext) {
        JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> meetupRawEvents =
            readMeetupRsvpsEvents(streamingContext);

        JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent;
        if (!cityToFilterName.isEmpty()) {
            meetupFilteredEvent = filterEventsByCountyAndCity(meetupRawEvents);
        } else {
            meetupFilteredEvent = filterEventsByCounty(meetupRawEvents);
        }

        JavaPairDStream<Integer, String> countPerTopicsInDescOrder =
            findCountPerTopicsInDescOrderWithWindow(meetupFilteredEvent);
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
        JavaPairDStream<Integer, String> countPerTopicsInDescOrder, int k,
        int windowDurationInMinutes, String outputFilePath) {
        countPerTopicsInDescOrder.foreachRDD(rdd -> {
            writeTopHashtagsToFile(
            new File(outputFilePath), k, windowDurationInMinutes, rdd);
        });
    }

    /**
     *
     * @param meetupFilteredEvent
     * @return
     */
    private JavaPairDStream<Integer, String> findCountPerTopicsInDescOrderWithWindow(
        JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent) {
        JavaPairDStream<String, Integer> meetupAggregatedEvent = findTotalOfEveryUrlKey(meetupFilteredEvent);
        JavaPairDStream<Integer, String> swappedPair = meetupAggregatedEvent.mapToPair(x -> x.swap());
        return swappedPair.transformToPair(jPairRDD -> jPairRDD.sortByKey(true));
    }

    /**
     *
     * @param meetupFilteredEvent
     * @return
     */
    private JavaPairDStream<String, Integer> findTotalOfEveryUrlKey(
        JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent) {
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
     * @return
     */
    private JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> readMeetupRsvpsEvents(
        JavaStreamingContext streamingContext) {
        return KafkaUtils.createDirectStream(
            streamingContext, LocationStrategies.PreferConsistent(),
            ConsumerStrategies.Subscribe(Collections.unmodifiableList(Arrays.asList(inputTopicName)),
            kafkaConsumerConfig.getKafkaConsumerProperties()));
    }

    /**
     *
     * @param meetupRawEvents
     * @return
     */
    private JavaDStream<MeetupRsvpsMessage> filterEventsByCountyAndCity(
        JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> meetupRawEvents) {
        return meetupRawEvents.map(
            v -> v.value()).filter(meetupRsvpsMessage -> !meetupRsvpsMessage.getGroup().getGroup_country()
            .equalsIgnoreCase(this.countryToFilter) && meetupRsvpsMessage.getGroup().getGroup_city()
            .equalsIgnoreCase(this.cityToFilterName));
    }

    /**
     *
     * @param meetupRawEvents
     * @return
     */
    private JavaDStream<MeetupRsvpsMessage> filterEventsByCounty(
            JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> meetupRawEvents) {
        return meetupRawEvents.map(
            v -> v.value()).filter(meetupRsvpsMessage -> !meetupRsvpsMessage.getGroup().getGroup_country()
            .equalsIgnoreCase(this.countryToFilter));
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
        Files.append("Top " + numberOfTrendingTopicsPerMinutes + " meetup topics captured from "
            + timeRange + ":\n", file, Charset.forName("UTF-8"));
        List<Tuple2<Integer, String>> topTrendTopics = trendTopicRdd.take(numberOfTrendingTopicsPerMinutes);

        topTrendTopics.forEach(countAndTopics -> {
            try {
                Files.append(countAndTopics.toString() + "\n", file, Charset.forName("UTF-8"));
            } catch (IOException e) {
                log.error("error while writing output to file");
            }
        });
    }
}