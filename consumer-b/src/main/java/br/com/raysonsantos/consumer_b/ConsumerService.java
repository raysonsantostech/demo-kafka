package br.com.raysonsantos.consumer_b;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {

    @KafkaListener(topics = "hello-topic", groupId = "hello-group")
    public void consume(String message) {
        System.out.println("Received message: " + message);
    }
}
