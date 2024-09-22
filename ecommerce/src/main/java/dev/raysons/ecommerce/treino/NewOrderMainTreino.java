package dev.raysons.ecommerce.treino;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.text.MessageFormat;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class NewOrderMainTreino {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        
        Callback callback = (data, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                return;                
            }

            var message = MessageFormat.format("\ntopic: {0}\tpartition: {1}\toffset: {2}\ttimestamp:{3}\n", 
                data.topic(), data.partition(), data.offset(), data.timestamp());
                
            System.out.println(message);
        };

        var producer = new KafkaProducer<String, String>(properties());

        for (int i = 0; i < 2000; i++) {
            var value = UUID.randomUUID().toString() + ", 445, 4445";
            var record = new ProducerRecord<String, String>("TREINO_ECOMMERCE_NEW_ORDER", value, value);
            producer.send(record, callback).get();
            
            var email = "email, Thanks! We are processing your things.";
            var emailRecord = new ProducerRecord<String, String>("TREINO_ECOMMERCE_SEND_EMAIL", email, email);
            producer.send(emailRecord, callback).get();
        }

        producer.close();
    }

    public static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        return properties;
    }

}
