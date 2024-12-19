package br.com.raysonsatos.producer_a.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class ProducerService {

    private static final String TOPIC_HELLO = "hello-topic";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<String> nomes = List.of("João", "Maria", "Pedro", "Ana", "Carlos");
    private final Random random = new Random();

    public ProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMessage() {
        String key = UUID.randomUUID().toString();
        String message = "Hello, " + getName();

        kafkaTemplate.send(TOPIC_HELLO, key, message);

        System.out.println("Publish message: " + message);
    }

    public void publishMessageWithRetry() {
        String key = UUID.randomUUID().toString();
        String message = "Hello, " + getName();

        kafkaTemplate.send(TOPIC_HELLO, key, message);

        System.out.println("Publish message: " + message);
    }

    private String getName() {
        return nomes.get(random.nextInt(nomes.size()));
    }
}
