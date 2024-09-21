package dev.raysons.ecommerce.treino;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class EmailServiceTreino {

    public static void main(String[] args) {
        var consumer = new KafkaConsumer<String, String>(properties());
        consumer.subscribe(Collections.singletonList("ECOMMERCE_EMAIL_TREINO"));

        while (true) {
            var records = consumer.poll(Duration.ofMillis(100));
    
            if (!records.isEmpty()) {
                System.out.println("Encontrei " + records.count() + " registros");
            }
    
            for (ConsumerRecord<String, String> data : records) {
                System.out.println("-----------------------------------------------------------------------------------");
                System.out.println("Processing....");
                var message = MessageFormat.format("\ntopic: {0}\tpartition: {1}\toffset: {2}\ttimestamp:{3}\nKey: {4}\nValue: {5}", data.topic(), data.partition(), data.offset(), data.timestamp(), data.key(), data.value());
                System.out.println(message);
            }
        }
    }

    private static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, EmailServiceTreino.class.getSimpleName());
        //properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, EmailServiceTreino.class.getSimpleName());

        return properties;
    }

}
