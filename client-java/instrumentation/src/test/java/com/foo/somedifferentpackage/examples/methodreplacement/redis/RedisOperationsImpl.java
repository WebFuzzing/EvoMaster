package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.evomaster.client.java.instrumentation.example.redis.RedisOperations;

public class RedisOperationsImpl implements RedisOperations {

    private final StatefulRedisConnection<String, String> connection;

    public RedisOperationsImpl(String uri) {
        System.setProperty("io.lettuce.core.jfr", "false");
        ClientResources resources = DefaultClientResources.builder()
                .build();
        RedisClient client = RedisClient.create(resources, uri);
        this.connection = client.connect();
    }

    @Override
    public String get(String key){
        RedisCommands<String, String> commands = connection.sync();
        return commands.get(key);
    }

    @Override
    public String hget(String key, String hash){
        RedisCommands<String, String> commands = connection.sync();
        return commands.hget(key, hash);
    }

}
