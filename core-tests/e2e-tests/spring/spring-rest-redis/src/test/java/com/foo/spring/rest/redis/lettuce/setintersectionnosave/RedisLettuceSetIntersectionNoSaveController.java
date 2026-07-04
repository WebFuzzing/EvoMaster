package com.foo.spring.rest.redis.lettuce.setintersectionnosave;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.setintersectionnosave.RedisLettuceSetIntersectionNoSaveApp;

public class RedisLettuceSetIntersectionNoSaveController extends RedisController {
    public RedisLettuceSetIntersectionNoSaveController() {
        super("lettuce", RedisLettuceSetIntersectionNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.setintersectionnosave";
    }
}
