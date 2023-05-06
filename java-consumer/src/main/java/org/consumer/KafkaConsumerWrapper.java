package org.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaConsumerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerWrapper.class);
    private final Consumer kafkaConsumer;

    public static final  ObjectMapper objectMapper = new ObjectMapper();
    private final DbClient dbClient;

    public static void main(String[] args) {
        logger.info("Starting Kafka consumer");
        new KafkaConsumerWrapper();
    }

    public KafkaConsumerWrapper() {

        String localhost = System.getenv().getOrDefault("LOCAL_INTERFACE", "0.0.0.0");
        Properties properties = getConfig(localhost);
        kafkaConsumer = new KafkaConsumer<>(properties);
        logger.info(System.getenv().toString());

        this.dbClient = new DbClient("jdbc:postgresql://" + localhost + ":5432/monitoring_results_db");
        init();
    }

    public void init() {
        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
            kafkaConsumer.wakeup();


        }));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing DB Client");
            dbClient.close();
            logger.info("DB Client closed");
        }));

        String topic = System.getProperty("TOPIC", "telemetry-topic");
        kafkaConsumer.subscribe(Arrays.asList(topic));
        pollForEvents();
    }
    public static Properties getConfig(String localhost) {
        String bootstrapServers = localhost + ":9092";
        String groupId = "consumer-group-id";

        logger.info("Kafka bootstrap servers: " + bootstrapServers);

        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers );
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }

    public static class TelemetryDataPoint {
        String deviceId;

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }

        public void setTs(long ts) {
            this.ts = ts;
        }

        public void setDoor(String door) {
            this.door = door;
        }

        float temperature;
        long ts;
        String door;
        public TelemetryDataPoint() {}
        public TelemetryDataPoint(String deviceId, float temperature, long ts, String door) {
            this.deviceId = deviceId;
            this.temperature = temperature;
            this.ts = ts;
            this.door = door;
        }
    }
    public void pollForEvents() {
        try {
            while (true) {
                ConsumerRecords<String, String> records =
                        kafkaConsumer.poll(Duration.ofMillis(100));

                List<TelemetryDataPoint> dataPoints = new ArrayList<>();

                for (ConsumerRecord<String, String> record : records) {
                    logger.info("Key: " + record.key() + ", Value: " + record.value());
                    logger.info("Partition: " + record.partition() + ", Offset:" + record.offset());

                    dataPoints.add(deserialize(record.value()));
                }
                if (!dataPoints.isEmpty()) {
                    dbClient.submitEventsForWrite(dataPoints);
                }
            }

        } catch (WakeupException e) {
            logger.info("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            logger.error("Unexpected exception", e);
        } finally {
            kafkaConsumer.close(); // this will also commit the offsets if need be.
            logger.info("The consumer is now gracefully closed.");
        }
    }

    public static TelemetryDataPoint deserialize(String value) {
        try {
            return objectMapper.readValue(value, TelemetryDataPoint.class);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
