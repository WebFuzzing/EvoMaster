package com.redis.jedis;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public abstract class AbstractRedisJedisRest {

    protected UnifiedJedis jedis;

    @PostConstruct
    public void init() {
        String redisHost = System.getProperty("spring.redis.host", "localhost");
        int redisPort = Integer.parseInt(System.getProperty("spring.redis.port", "6379"));
        jedis = new UnifiedJedis(new HostAndPort(redisHost, redisPort));
    }

    @PreDestroy
    public void shutdown() {
        jedis.close();
    }

    /**
     * Whether the failure is Redis complaining that an index was already created,
     * which is what makes index creation safe to repeat.
     */
    protected static boolean isIndexAlreadyExists(JedisDataException e) {
        return e.getMessage() != null && e.getMessage().contains("already exists");
    }
}
