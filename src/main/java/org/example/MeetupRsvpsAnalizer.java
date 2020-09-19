package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import com.google.common.io.Files;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Seconds;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.example.deserializer.MeetupRsvpsDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.example.model.GroupTopics;
import org.example.model.MeetupRsvpsMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import scala.Tuple2;

public class MeetupRsvpsAnalizer {
    private static final String RUN_LOCAL_WITH_AVAILABLE_CORES = "spark://spark-master:7077";
    private static final String APPLICATION_NAME = "Meetup Rsvps Analizer";
    private static final String CASE_SENSITIVE = "false";
    private static final int BATCH_DURATION_INTERVAL_MS = 10000;
    private static final Map<String, Object> KAFKA_CONSUMER_PROPERTIES;
    private static final String KAFKA_BROKERS = "kafka-broker:9092";
    private static final String KAFKA_OFFSET_RESET_TYPE = "latest";
    private static final String KAFKA_GROUP = "meetupGroup";
    private static final String KAFKA_TOPIC = "meetup-rsvps-topic";
    private static final Collection<String> TOPICS =
            Collections.unmodifiableList(Arrays.asList(KAFKA_TOPIC));
    private static final int NUMBER_TRENDING_TOPICS = 3;
    private static final String FILTER_BY_COUNTRY = "us";
    private static final String FILTER_BY_CITY = "Vilnius";
    private static int windowDurationInSec = 30; //Top hashtags and tweets in 5 minutes
    private static final String timeZoneId = "UTC";

    private static final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");


    static {
        Map<String, Object> kafkaProperties = new HashMap<>();
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        kafkaProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MeetupRsvpsDeserializer.class);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP);
        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KAFKA_OFFSET_RESET_TYPE);
        kafkaProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        KAFKA_CONSUMER_PROPERTIES = Collections.unmodifiableMap(kafkaProperties);
    }

    public static void main(String[] args) throws InterruptedException {

        final SparkConf conf = new SparkConf()
                .setMaster(RUN_LOCAL_WITH_AVAILABLE_CORES)
                .setAppName(APPLICATION_NAME)
                .set("spark.sql.caseSensitive", CASE_SENSITIVE);

        JavaStreamingContext streamingContext = new JavaStreamingContext(conf,
                new Duration(BATCH_DURATION_INTERVAL_MS));

        JavaInputDStream<ConsumerRecord<String, MeetupRsvpsMessage>> meetupRawEvents =
                KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(TOPICS, KAFKA_CONSUMER_PROPERTIES)
                );

        JavaDStream<MeetupRsvpsMessage> meetupFilteredEvent = meetupRawEvents.map(
                v -> v.value()).filter(meetupRsvpsMessage -> {
                    return !meetupRsvpsMessage.getGroup().getGroup_country().equalsIgnoreCase(FILTER_BY_COUNTRY);
                            //&& meetupRsvpsMessage.getGroup().getGroup_city().equalsIgnoreCase(FILTER_BY_CITY);
                });

        JavaPairDStream<String, Integer> urlkeyCountPairStream = meetupFilteredEvent.flatMapToPair(new PairFlatMapFunction<MeetupRsvpsMessage, String, Integer>() {
            @Override
            public Iterator<Tuple2<String, Integer>> call(MeetupRsvpsMessage meetupRsvpsMessage) throws Exception {
                List<Tuple2<String, Integer>> urlkeyCountTuples = new ArrayList<>();
                for (GroupTopics group_topic : meetupRsvpsMessage.getGroup().getGroup_topics()) {
                    urlkeyCountTuples.add(new Tuple2(group_topic.getUrlkey().toLowerCase(), 1));
                }
                return urlkeyCountTuples.iterator();
            }
        });
        JavaPairDStream<String, Integer>  keyCount = urlkeyCountPairStream.reduceByKeyAndWindow(
                (integer, integer2) -> integer + integer2, Seconds.apply(windowDurationInSec));

        JavaPairDStream<Integer, String> swappedPair =keyCount.mapToPair(x -> x.swap());
        JavaPairDStream<Integer,String> sortedStream = swappedPair
                .transformToPair(jPairRDD -> jPairRDD.sortByKey(false));

        sortedStream.print();
        System.out.println(sortedStream.count());

        StructType schema = DataTypes.createStructType(new StructField[] {
                DataTypes.createStructField("urlkey", DataTypes.StringType, true),
                DataTypes.createStructField("count", DataTypes.IntegerType, true)
        });
        File hashtagAndTweetOutputFile = new File("/home/trendtopics_" + System.currentTimeMillis()); //output file

        sortedStream.foreachRDD(rdd -> {
            writeTopHashtagsToFile(hashtagAndTweetOutputFile, 5, rdd);
//            SparkSession spark = JavaSparkSessionSingleton.getInstance(rdd.context().getConf());
//            Dataset<Row> msgDataFrame = spark.createDataFrame(rowRdd, schema);
//            msgDataFrame.show();
//            msgDataFrame.toJSON().printSchema();
        });

        streamingContext.start();
        streamingContext.awaitTermination();
    }
    private static void writeTopHashtagsToFile(File file, int numTopHashtags,
                                               JavaPairRDD<Integer, String> hashtagRDD) throws IOException {
        DateTime currentTime = DateTime.now(DateTimeZone.forID(timeZoneId));
        String timeRange = dateTimeFormat.print(currentTime.minusSeconds(windowDurationInSec))
                + " to " + dateTimeFormat.print(currentTime) + " (Pacific Time)";
        Files.append("Top " + numTopHashtags + " hashtags captured from " + timeRange + ":\n",
                file, Charset.forName("UTF-8"));

        // Get top {numTopHashtags} hashtags as (count, hashtag) tuple.
        List<Tuple2<Integer, String>> topHashtags = hashtagRDD.take(numTopHashtags);
        topHashtags.forEach(countAndHashtag -> {
            try {
                // write count and hashtag to the end of file
                Files.append(countAndHashtag.toString() + "\n", file, Charset.forName("UTF-8"));
            } catch (IOException e) {
                System.err.println("error while writing output to file");
            }
        });
    }
}

//
//class JavaSparkSessionSingleton {
//    private static transient SparkSession instance = null;
//    public static SparkSession getInstance(SparkConf sparkConf) {
//        if (instance == null) {
//            instance = SparkSession
//                    .builder()
//                    .config(sparkConf)
//                    .getOrCreate();
//        }
//        return instance;
//    }
//}