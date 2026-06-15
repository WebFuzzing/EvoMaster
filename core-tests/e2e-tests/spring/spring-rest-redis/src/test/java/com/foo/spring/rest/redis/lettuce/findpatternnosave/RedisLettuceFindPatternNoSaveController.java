package com.foo.spring.rest.redis.lettuce.findpatternnosave;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.findpatternnosave.RedisLettuceFindPatternNoSaveApp;

public class RedisLettuceFindPatternNoSaveController extends RedisController {
    public RedisLettuceFindPatternNoSaveController() {
        super("lettuce", RedisLettuceFindPatternNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.findpatternnosave";
    }
}
