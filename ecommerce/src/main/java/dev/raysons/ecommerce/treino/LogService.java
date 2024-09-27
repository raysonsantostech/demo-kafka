package dev.raysons.ecommerce.treino;

import java.util.regex.Pattern;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import dev.raysons.ecommerce.treino.kafka.KafkaServiceTreino;

public class LogService {

    public static void main(String[] args) {
        var logService = new LogService();
        try(var service = new KafkaServiceTreino<String>(LogService.class.getSimpleName(), Pattern.compile(".*ECOMMERCE.*"), 
        logService::parser, String.class, Map.of(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()))) {
            service.run();
        }
    }

    private void parser(ConsumerRecord<String, String> record) {
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
