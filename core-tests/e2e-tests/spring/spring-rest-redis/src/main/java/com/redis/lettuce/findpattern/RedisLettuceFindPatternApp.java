package com.redis.lettuce.findpattern;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisLettuceFindPatternApp extends SwaggerConfiguration {
    public RedisLettuceFindPatternApp() {
        super("redislettucefindpattern");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLettuceFindPatternApp.class, args);
    }

}
