package dev.raysons.ecommerce.treino;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class LogService {

    public static void main(String[] args) {
        var consumer = new KafkaConsumer<String, String>(properties());
        consumer.subscribe(Pattern.compile(".*ECOMMERCE.*"));

        while (true) {
            var records = consumer.poll(Duration.ofMillis(100));

            if (!records.isEmpty()) {
                System.out.println("Total de mensagens " + records.count() + " encontrados");
            }

            for (ConsumerRecord<String, String> record : records) {
                System.out.println("-------------------------------------------------------------------------");
                System.out.println("LOG");
                System.out.print("topic: " + record.topic() + "\t");
                System.out.print("partition:" + record.partition() + "\t");
                System.out.print("offset: " + record.offset() + "\t");
                System.out.print("timestamp: " + record.timestamp() + "\t");
                System.out.print("key: " + record.key() + "\t");
                System.out.print("value: " + record.value() + "\t");
                System.out.println();
            }
        }
    }

    private static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, LogService.class.getSimpleName());

        return properties;
    }

}
