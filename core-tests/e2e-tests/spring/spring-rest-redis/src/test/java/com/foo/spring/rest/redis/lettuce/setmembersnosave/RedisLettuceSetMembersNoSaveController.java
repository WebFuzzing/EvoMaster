package com.foo.spring.rest.redis.lettuce.setmembersnosave;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.setmembersnosave.RedisLettuceSetMembersNoSaveApp;

public class RedisLettuceSetMembersNoSaveController extends RedisController {
    public RedisLettuceSetMembersNoSaveController() {
        super("lettuce", RedisLettuceSetMembersNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.setmembersnosave";
    }
}
