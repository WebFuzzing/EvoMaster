package com.foo.spring.rest.redis.lettuce.setmembers;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.setmembers.RedisLettuceSetMembersApp;

public class RedisLettuceSetMembersController extends RedisController {
    public RedisLettuceSetMembersController() {
        super("lettuce", RedisLettuceSetMembersApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.setmembers";
    }
}
