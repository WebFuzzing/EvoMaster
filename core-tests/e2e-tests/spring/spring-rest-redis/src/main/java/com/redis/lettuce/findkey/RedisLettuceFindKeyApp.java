package com.redis.lettuce.findkey;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisLettuceFindKeyApp extends SwaggerConfiguration {
    public RedisLettuceFindKeyApp() {
        super("redislettucefindkey");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLettuceFindKeyApp.class, args);
    }

}
