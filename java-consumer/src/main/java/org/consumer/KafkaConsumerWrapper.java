package org.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaConsumerWrapper {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerWrapper.class);
    private final Consumer kafkaConsumer;
    public static void main(String[] args) {
        log.info("Starting Kafka consumer");
        new KafkaConsumerWrapper()
    }

    public KafkaConsumerWrapper() {

        Properties properties = getConfig();
        kafkaConsumer = new KafkaConsumer<>(properties);
        init();

    }

    public void init() {
        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
            kafkaConsumer.wakeup();

        }));

        String topic = System.getProperty("TOPIC", "telemetry-topic");
        kafkaConsumer.subscribe(Arrays.asList(topic));
        pollForEvents();

    }
    public static Properties getConfig() {
        String bootstrapServers = System.getProperty("BOOTSTRAP_SERVERS", "127.0.0.1:9092");
        String groupId = "consumer-group-id";

        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }

    public void pollForEvents() {
        try {
            while (true) {
                ConsumerRecords<String, String> records =
                        kafkaConsumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    log.info("Key: " + record.key() + ", Value: " + record.value());
                    log.info("Partition: " + record.partition() + ", Offset:" + record.offset());
                }
            }

        } catch (WakeupException e) {
            log.info("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            log.error("Unexpected exception", e);
        } finally {
            kafkaConsumer.close(); // this will also commit the offsets if need be.
            log.info("The consumer is now gracefully closed.");
        }
    }
}
