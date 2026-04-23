package com.foo.spring.rest.redis.lettuce.findkeynosave.findkey;

import com.foo.spring.rest.redis.RedisController;
import com.redis.lettuce.findkeynosave.findkey.RedisLettuceFindKeyNoSaveApp;

public class RedisLettuceFindKeyNoSaveController extends RedisController {
    public RedisLettuceFindKeyNoSaveController() {
        super("lettuce", RedisLettuceFindKeyNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.redis.lettuce.findkeynosave";
    }
}
