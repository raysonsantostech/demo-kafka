package dev.raysons.ecommerce.treino;

import java.text.MessageFormat;

import dev.raysons.ecommerce.treino.kafka.ConsumerFunctional;
import dev.raysons.ecommerce.treino.kafka.KafkaService;

public class EmailServiceTreino {

    public static void main(String[] args) {
        ConsumerFunctional parser = (data) -> {
            System.out.println("-----------------------------------------------------------------------------------");
            System.out.println("Processing....");
            var message = MessageFormat.format(
                    "\ntopic: {0}\tpartition: {1}\toffset: {2}\ttimestamp:{3}\nKey: {4}\nValue: {5}", data.topic(),
                    data.partition(), data.offset(), data.timestamp(), data.key(), data.value());
            System.out.println(message);
        };    
        new KafkaService("TREINO_ECOMMERCE_SEND_EMAIL", parser, EmailServiceTreino.class.getSimpleName());
    }
}
