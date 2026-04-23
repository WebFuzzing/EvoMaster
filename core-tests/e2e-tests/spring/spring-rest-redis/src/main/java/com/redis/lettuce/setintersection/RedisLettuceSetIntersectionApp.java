package com.redis.lettuce.setintersection;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisLettuceSetIntersectionApp extends SwaggerConfiguration {
    public RedisLettuceSetIntersectionApp() {
        super("redislettucesetintersection");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLettuceSetIntersectionApp.class, args);
    }

}
