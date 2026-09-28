package com.foo.spring.rest.redis.jedis.aggregatenosave;

import com.foo.spring.rest.redis.RedisController;
import com.redis.jedis.aggregatenosave.RedisJedisAggregateNoSaveApp;

public class RedisJedisAggregateNoSaveController extends RedisController {
    public RedisJedisAggregateNoSaveController() {
        super("jedis", RedisJedisAggregateNoSaveApp.class, REDIS_STACK_IMAGE);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.jedis.aggregatenosave";
    }
}
