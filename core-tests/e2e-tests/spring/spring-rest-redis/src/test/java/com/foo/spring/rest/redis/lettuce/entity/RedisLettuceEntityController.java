package com.foo.spring.rest.redis.lettuce.entity;

import com.example.redis.lettuce.entity.EntityApp;
import com.foo.spring.rest.redis.RedisController;

public class RedisLettuceEntityController extends RedisController {
    public RedisLettuceEntityController() {
        super("lettuce", EntityApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.redis.lettuce.entity";
    }
}
