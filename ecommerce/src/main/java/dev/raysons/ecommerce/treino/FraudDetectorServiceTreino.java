package dev.raysons.ecommerce.treino;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import dev.raysons.ecommerce.aula.FraudDetectorService;
import dev.raysons.ecommerce.treino.kafka.KafkaServiceTreino;
import dev.raysons.ecommerce.treino.model.Order;
import java.util.Map;

public class FraudDetectorServiceTreino {

    public static void main(String[] args) throws InterruptedException {
        var fraudDetectorService = new FraudDetectorServiceTreino();
        try (var service = new KafkaServiceTreino<Order>(FraudDetectorService.class.getSimpleName(), "TREINO_ECOMMERCE_NEW_ORDER", fraudDetectorService::parser, Order.class, Map.of())) {
            service.run();
        }
    }

    private void parser(ConsumerRecord<String, Order> record) {
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Processando ordem (Treino)");
        System.out.print("topic: " + record.topic() + "\t");
        System.out.print("partition:" + record.partition() + "\t");
        System.out.print("offset: " + record.offset() + "\t");
        System.out.print("timestamp: " + record.timestamp() + "\t");
        System.out.print("key: " + record.key() + "\t");
        System.out.print("value: " + record.value() + "\t");
        System.out.println();

        try {
            Thread.sleep(Duration.ofSeconds(1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
