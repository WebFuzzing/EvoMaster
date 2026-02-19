package com.redis.lettuce.findhash;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisLettuceFindHashApp extends SwaggerConfiguration {
    public RedisLettuceFindHashApp() {
        super("redislettucefindhash");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLettuceFindHashApp.class, args);
    }

}
