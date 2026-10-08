package com.foo.spring.rest.redis.jedis.aggregate;

import com.foo.spring.rest.redis.RedisController;
import com.redis.jedis.aggregate.RedisJedisAggregateApp;

public class RedisJedisAggregateController extends RedisController {
    public RedisJedisAggregateController() {
        super("jedis", RedisJedisAggregateApp.class, REDIS_STACK_IMAGE);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.jedis.aggregate";
    }
}
