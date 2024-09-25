package dev.raysons.ecommerce.treino.kafka;

import java.io.Closeable;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaDispatcher implements Closeable {

    private final KafkaProducer<String, String> producer;

    public KafkaDispatcher() {
        this.producer = new KafkaProducer<>(properties());
    }

    public void send(String topic, String key, String value) throws InterruptedException, ExecutionException {
        Callback callback = (data, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                return;                
            }

            var message = MessageFormat.format("\ntopic: {0}\tpartition: {1}\toffset: {2}\ttimestamp:{3}\n", 
                data.topic(), data.partition(), data.offset(), data.timestamp());

            System.out.println(message);
        };


        var record = new ProducerRecord<>(topic, key, value);
        this.producer.send(record, callback).get();
    }

    private static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }

    @Override
    public void close() {
        this.producer.close();
    }
    
}
