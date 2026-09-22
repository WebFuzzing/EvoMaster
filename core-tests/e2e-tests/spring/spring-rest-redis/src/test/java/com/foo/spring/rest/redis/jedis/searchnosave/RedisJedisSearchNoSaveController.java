package com.foo.spring.rest.redis.jedis.searchnosave;

import com.foo.spring.rest.redis.RedisController;
import com.redis.jedis.searchnosave.RedisJedisSearchNoSaveApp;

public class RedisJedisSearchNoSaveController extends RedisController {
    public RedisJedisSearchNoSaveController() {
        super("jedis", RedisJedisSearchNoSaveApp.class, REDIS_STACK_IMAGE);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.jedis.searchnosave";
    }
}
