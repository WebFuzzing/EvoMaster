package com.foo.spring.rest.redis.jedis.search;

import com.foo.spring.rest.redis.RedisController;
import com.redis.jedis.search.RedisJedisSearchApp;

public class RedisJedisSearchController extends RedisController {
    public RedisJedisSearchController() {
        super("jedis", RedisJedisSearchApp.class, REDIS_STACK_IMAGE);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.jedis.search";
    }
}
