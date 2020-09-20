package org.example.config;

import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.deserializer.MeetupRsvpsDeserializer;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/*
This class configures the kafka consumer. Ideally it should able to read properties from config file.
 */
@Data
public class KafkaConsumerConfig implements Serializable {
    private String kafkaBrokers;
    private String kafkaGroup;
    private Map<String, Object> kafkaConsumerProperties;

    public KafkaConsumerConfig(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
        Map<String, Object> kafkaProperties = new HashMap<>();
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        kafkaProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MeetupRsvpsDeserializer.class);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "meetupGroup");
        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        this.kafkaConsumerProperties = Collections.unmodifiableMap(kafkaProperties);
    }

    public Map<String, Object> getKafkaConsumerProperties() {
        return kafkaConsumerProperties;
    }
}
