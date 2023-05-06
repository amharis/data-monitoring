package org.consumer;

import org.junit.Assert;
import org.junit.Test;

public class KafkaConsumerWrapperTest {
    @Test
    public void testDeserialize() {
        String input = "{\"ts\": 1683273850, \"deviceId\": \"device1\", \"temperature\": 1.48, \"door\": \"close\"}";
        KafkaConsumerWrapper.TelemetryDataPoint dataPoint= KafkaConsumerWrapper.deserialize(input);
        Assert.assertNotNull(dataPoint);
    }

    @Test
    public void testPrepareQueryString() {
        String input = "{\"ts\": 1683273850, \"deviceId\": \"device1\", \"temperature\": 1.48, \"door\": \"close\"}";
        KafkaConsumerWrapper.TelemetryDataPoint dataPoint= KafkaConsumerWrapper.deserialize(input);
        String preparedQuery = DbClient.getQueryString(dataPoint);
        System.out.println("Result: " + preparedQuery);
    }
}
