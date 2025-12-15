package com.foo.spring.rest.redis.lettuce;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.RedisLettuceApp;

public class RedisLettuceAppController extends RedisController {
    public RedisLettuceAppController() {
        super("lettuce", RedisLettuceApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce";
    }
}
