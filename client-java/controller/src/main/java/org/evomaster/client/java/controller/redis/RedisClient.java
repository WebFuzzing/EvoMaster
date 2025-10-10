package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.instrumentation.RedisCommand;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Set;
import java.util.stream.Collectors;

public class RedisClient {

    private final RedisTemplate<String, String> template;

    public RedisClient(RedisConnectionFactory factory) {
        this.template = new RedisTemplate<>();
        this.template.setConnectionFactory(factory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        this.template.setKeySerializer(stringSerializer);
        this.template.setValueSerializer(stringSerializer);
        this.template.setHashKeySerializer(stringSerializer);
        this.template.setHashValueSerializer(stringSerializer);
        this.template.afterPropertiesSet();
    }

    public Set<String> getKeysByType(String expectedType) {
        return template.keys("*").stream().filter(
                k -> template.type(k).name().equalsIgnoreCase(expectedType)
        ).collect(Collectors.toSet());
    }

    public Set<String> getAllKeys() {
        return template.keys("*");
    }

    public String getType(String key){
        return String.valueOf(template.type(key));
    }

    public Boolean hashFieldExists(String k, String targetField){
        return template.opsForHash().hasKey(k, targetField);
    }

    public Set<String> getSetMembers(String key){
        return template.opsForSet().members(key);
    }

    public void setValue(String key, String value){
        template.opsForValue().set(key, value);
    }
}