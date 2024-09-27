package dev.raysons.ecommerce.treino;

import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import dev.raysons.ecommerce.treino.kafka.KafkaServiceTreino;

public class LogService {

    public static void main(String[] args) {
        var logService = new LogService();
        try(var service = new KafkaServiceTreino(LogService.class.getSimpleName(), Pattern.compile(".*ECOMMERCE.*"), logService::parser)) {
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
