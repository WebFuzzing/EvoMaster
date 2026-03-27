package com.foo.spring.rest.redis.lettuce.findhash;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.findhash.RedisLettuceFindHashApp;

public class RedisLettuceFindHashController extends RedisController {
    public RedisLettuceFindHashController() {
        super("lettuce", RedisLettuceFindHashApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.findhash";
    }
}
