package com.foo.spring.rest.redis.lettuce.findhashnosave;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.findhashnosave.RedisLettuceFindHashNoSaveApp;

public class RedisLettuceFindHashNoSaveController extends RedisController {
    public RedisLettuceFindHashNoSaveController() {
        super("lettuce", RedisLettuceFindHashNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.findhashnosave";
    }
}
