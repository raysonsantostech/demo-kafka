package dev.raysons.ecommerce.treino;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import dev.raysons.ecommerce.treino.kafka.KafkaDispatcher;
import dev.raysons.ecommerce.treino.model.Order;

public class NewOrderMainTreino {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        try(var orderDispatcher = new KafkaDispatcher<Order>()) {
            for (int i = 0; i < 20; i++) {
                var key = UUID.randomUUID().toString();
                var userId = UUID.randomUUID().toString();
                var orderId = UUID.randomUUID().toString();
                var amount = new BigDecimal(Math.random() * 5000 + 1);
                var order = new Order(userId, orderId, amount);
    
                orderDispatcher.send("TREINO_ECOMMERCE_NEW_ORDER", key, order);
            }
        }

        
        try(var emailDispatcher = new KafkaDispatcher<String>()) {
            for (int i = 0; i < 20; i++) {
                var key = UUID.randomUUID().toString();
                var email = "email, Thanks! We are processing your things.";

                emailDispatcher.send("TREINO_ECOMMERCE_SEND_EMAIL", key, email);
            }
        }

    }

}
