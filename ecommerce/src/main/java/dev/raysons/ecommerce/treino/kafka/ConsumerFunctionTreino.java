package dev.raysons.ecommerce.treino.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ConsumerFunctionTreino {
    
    public void consumer(ConsumerRecord<String, String> records);
    
}
