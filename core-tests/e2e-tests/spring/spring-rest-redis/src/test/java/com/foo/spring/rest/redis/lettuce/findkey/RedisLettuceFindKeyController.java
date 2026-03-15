package com.foo.spring.rest.redis.lettuce.findkey;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.findkey.RedisLettuceFindKeyApp;

public class RedisLettuceFindKeyController extends RedisController {
    public RedisLettuceFindKeyController() {
        super("lettuce", RedisLettuceFindKeyApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.findkey";
    }
}
