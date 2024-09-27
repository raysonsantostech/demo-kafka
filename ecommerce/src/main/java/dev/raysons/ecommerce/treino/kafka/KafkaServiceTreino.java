package dev.raysons.ecommerce.treino.kafka;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import dev.raysons.ecommerce.treino.gson.GsonDeserializer;

public class KafkaServiceTreino<T> implements Closeable {
    private final KafkaConsumer<String, T> consumer;
    private final ConsumerFunctionTreino<T> parser;

    public KafkaServiceTreino(String groupId, String topic, ConsumerFunctionTreino<T> parser, Class<T> type, Map<String, String> customProperties) {
        this(groupId, parser, type, customProperties);
        this.consumer.subscribe(Collections.singletonList(topic));
    }

    public KafkaServiceTreino(String groupId, Pattern topic, ConsumerFunctionTreino<T> parser, Class<T> type, Map<String, String> customProperties) {
        this(groupId, parser, type, customProperties);
        this.consumer.subscribe(topic);
    }

    private KafkaServiceTreino(String groupId, ConsumerFunctionTreino<T> parser, Class<T> type, Map<String, String> customProperties) {
        this.consumer = new KafkaConsumer<String, T>(properties(groupId, type, customProperties));
        this.parser = parser;
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

    private Properties properties(String groupId, Class<T> type, Map<String, String> overrideProperties) {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        //properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GsonDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        //properties.setProperty(GsonDeserializer.TYPE, String.class.getName());
        properties.setProperty(GsonDeserializer.TYPE, type.getName());
        properties.putAll(overrideProperties);

        return properties;
    }

    @Override
    public void close() {
        this.consumer.close();
    }

}
