package dev.raysons.ecommerce.treino.gson;

import org.apache.kafka.common.serialization.Serializer;

import com.google.gson.Gson;

public class GsonSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, Object data) {
        return new Gson().toJson(data).getBytes();
    }

}
