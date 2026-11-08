package com.foo.spring.rest.redis.lettuce.richentity;

import com.example.redis.lettuce.richentity.RichEntityApp;
import com.foo.spring.rest.redis.RedisController;

public class RedisLettuceRichEntityController extends RedisController {
    public RedisLettuceRichEntityController() {
        super("lettuce", RichEntityApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.redis.lettuce.richentity";
    }
}
