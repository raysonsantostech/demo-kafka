package dev.raysons.ecommerce.treino;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import dev.raysons.ecommerce.treino.kafka.KafkaDispatcher;

public class NewOrderMainTreino {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        var dispatcher = new KafkaDispatcher();

        for (int i = 0; i < 200; i++) {
            var key = UUID.randomUUID().toString();

            var value = key + ", 445, 4445";
            dispatcher.send("TREINO_ECOMMERCE_NEW_ORDER", key, value);
            
            var email = key + "email, Thanks! We are processing your things.";
            dispatcher.send("TREINO_ECOMMERCE_SEND_EMAIL", key, email);
        }
    }

}
