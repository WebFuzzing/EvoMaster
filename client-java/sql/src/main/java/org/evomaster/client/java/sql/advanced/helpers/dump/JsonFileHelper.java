package org.evomaster.client.java.sql.advanced.helpers.dump;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class JsonFileHelper {

    public static void saveObject(String path, Object object){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            objectMapper.writeValue(new File(path), object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T loadObject(String path, Class<T> valueType){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return objectMapper.readValue(new File(path), valueType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
