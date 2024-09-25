package dev.raysons.ecommerce.treino.kafka;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaServiceTreino implements Closeable {
    private final KafkaConsumer<String, String> consumer;
    private final ConsumerFunctionTreino parser;

    public KafkaServiceTreino(String groupId, String topic, ConsumerFunctionTreino parser) {
        this.consumer = new KafkaConsumer<String, String>(properties(groupId));
        this.parser = parser;
        this.consumer.subscribe(Collections.singletonList(topic));
    }

    public void run() {
        while (true) {
            var records = this.consumer.poll(Duration.ofMillis(100));

            if (!records.isEmpty()) {
                System.out.println("Encontrei " + records.count() + " registros");
            }

            records.forEach(parser::consumer);
        }

    }

    private static Properties properties(String groupId) {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");

        return properties;
    }

    @Override
    public void close() {
        this.consumer.close();
    }

}
