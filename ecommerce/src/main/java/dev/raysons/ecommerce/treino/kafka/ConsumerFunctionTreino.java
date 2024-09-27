package dev.raysons.ecommerce.treino.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ConsumerFunctionTreino<T> {
    
    public void consumer(ConsumerRecord<String, T> records);
    
}
