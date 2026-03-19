package com.foo.spring.rest.redis.lettuce.findpattern;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.findpattern.RedisLettuceFindPatternApp;

public class RedisLettuceFindPatternController extends RedisController {
    public RedisLettuceFindPatternController() {
        super("lettuce", RedisLettuceFindPatternApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.findpattern";
    }
}
