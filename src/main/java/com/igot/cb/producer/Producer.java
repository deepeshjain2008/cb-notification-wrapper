package com.igot.cb.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class Producer {
    Logger logger = LogManager.getLogger(Producer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public Producer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void push(String topic, Object value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String message = mapper.writeValueAsString(value);
            logger.info("Pushing to Kafka topic '{}': {}", topic, message);
            kafkaTemplate.send(topic, message);
        } catch (JsonProcessingException e) {
            logger.error("Exception while serializing the value", e);
        } catch (Exception e) {
            logger.error("Exception in sending message", e);
        }
    }
}
