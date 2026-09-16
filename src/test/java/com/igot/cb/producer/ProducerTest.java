package com.igot.cb.producer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.mockito.Mockito.*;

class ProducerTest {

    private Producer producer;
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new Producer(kafkaTemplate);
    }

    @Test
    void testPush_success() {
        doReturn(null).when(kafkaTemplate).send(anyString(), anyString());

        Map<String, Object> payload = Map.of("key", "value");

        producer.push("test-topic", payload);

        verify(kafkaTemplate, times(1)).send(eq("test-topic"), anyString());
    }

    @Test
    void testPush_jsonProcessingException() {
        // Create a payload that Jackson cannot serialize (ObjectMapper would normally throw on cyclic references, but
        // since it is re-created inside the method we cannot mock it directly. So we mock kafkaTemplate to throw and let the catch handle it.)

        Object badObject = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("bad toString");
            }
        };

        producer.push("test-topic", badObject);

        // no exception is thrown out
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void testPush_kafkaTemplateThrows() {
        doThrow(new RuntimeException("Kafka error")).when(kafkaTemplate).send(anyString(), anyString());

        Map<String, Object> payload = Map.of("key", "value");

        producer.push("test-topic", payload);

        verify(kafkaTemplate, times(1)).send(eq("test-topic"), anyString());
    }
}
