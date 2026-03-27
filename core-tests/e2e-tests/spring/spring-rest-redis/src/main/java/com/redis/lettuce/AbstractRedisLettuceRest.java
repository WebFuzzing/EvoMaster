package com.redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public abstract class AbstractRedisLettuceRest {

    protected RedisCommands<String, String> sync;

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;

    @PostConstruct
    public void init() {
        String redisHost = System.getProperty("spring.redis.host", "localhost");
        String redisPort = System.getProperty("spring.redis.port", "6379");
        String redisUri = "redis://" + redisHost + ":" + redisPort;
        redisClient = RedisClient.create(redisUri);
        connection = redisClient.connect();
        sync = connection.sync();
    }

    @PreDestroy
    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }
}
