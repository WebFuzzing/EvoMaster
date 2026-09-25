package com.redis.jedis.aggregate;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisJedisAggregateApp extends SwaggerConfiguration {
    public RedisJedisAggregateApp() {
        super("redisjedisaggregate");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisJedisAggregateApp.class, args);
    }

}
