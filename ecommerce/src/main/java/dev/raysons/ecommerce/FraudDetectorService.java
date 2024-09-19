package dev.raysons.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;
import java.time.Duration;
import java.util.Collections;

public class FraudDetectorService {

    public static void main(String[] args) {
        var consumer = new KafkaConsumer<String, String>(properties());
        consumer.subscribe(Collections.singletonList("TREINO_ECOMMERCE_NEW_ORDER"));

        while(true) {
            var records = consumer.poll(Duration.ofMillis(100)); 

            if (!records.isEmpty()) {
                System.out.println("Total " + records.count() + " registros");
            }

            for (ConsumerRecord<String, String> record : records) {
                System.out.println("-----------------------------------------");
                System.out.println("Iniciando analise");
                System.out.println("topic: " + record.topic());
                System.out.println("partition: " + record.partition());
                System.out.println("offset: " + record.offset());
                System.out.println("timestamp: " + record.timestamp());
                System.out.println("value: " + record.value());
            }
        }
    }

    public static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, FraudDetectorService.class.getSimpleName());
        return properties;        
    }

}
