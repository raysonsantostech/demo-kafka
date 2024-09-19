package dev.raysons.ecommerce.treino;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class NewOrderMainTreino {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        var value = "45, 445, 4445";
        var record = new ProducerRecord<String, String>("TREINO_ECOMMERCE_NEW_ORDER", value, value);

        var producer = new KafkaProducer<String, String>(properties());
        producer.send(record, (data, ex) -> {
            if(ex != null) {
                ex.printStackTrace();
                return;
            }

            System.out.println("topic :" + data.topic());
            System.out.println("partition :" + data.partition());
            System.out.println("offset :" + data.offset());
            System.out.println("timestamp :" + data.timestamp());
        }).get();
    }

    public static Properties properties() {
        var properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        return properties;
    }

}
