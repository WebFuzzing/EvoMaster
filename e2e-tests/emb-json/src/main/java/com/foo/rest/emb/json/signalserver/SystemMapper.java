package com.foo.rest.emb.json.signalserver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.annotation.Nonnull;

/**
 * This code is taken from Signal-Server
 * G: https://github.com/signalapp/Signal-Server
 * L: MIT
 * P: signal-server/service/src/main/java/org/whispersystems/textsecuregcm/util/SystemMapper.java
 */
public class SystemMapper {

    private static final ObjectMapper JSON_MAPPER = configureMapper(new ObjectMapper());

    @Nonnull
    public static ObjectMapper jsonMapper() {
        return JSON_MAPPER;
    }

    public static ObjectMapper configureMapper(final ObjectMapper mapper) {
        return mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setFilterProvider(new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.serializeAll()))
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .registerModules(
//                        SecretsModule.INSTANCE,
                        new JavaTimeModule(),
                        new Jdk8Module());
    }
}
