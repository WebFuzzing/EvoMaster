package com.foo.spring.rest.redis.lettuce.setintersection;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.setintersection.RedisLettuceSetIntersectionApp;

public class RedisLettuceSetIntersectionController extends RedisController {
    public RedisLettuceSetIntersectionController() {
        super("lettuce", RedisLettuceSetIntersectionApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.setintersection";
    }
}
