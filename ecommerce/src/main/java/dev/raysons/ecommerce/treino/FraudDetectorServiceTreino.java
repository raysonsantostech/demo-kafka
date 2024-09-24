package dev.raysons.ecommerce.treino;

import java.time.Duration;

import dev.raysons.ecommerce.aula.FraudDetectorService;
import dev.raysons.ecommerce.treino.kafka.ConsumerFunctional;
import dev.raysons.ecommerce.treino.kafka.KafkaService;

public class FraudDetectorServiceTreino {

    public static void main(String[] args) throws InterruptedException {
        ConsumerFunctional parser = (record) -> {
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
        };

        new KafkaService("TREINO_ECOMMERCE_NEW_ORDER", parser, FraudDetectorService.class.getSimpleName());
    }

}
