package com.redis.lettuce.findhashnosave;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisLettuceFindHashNoSaveApp extends SwaggerConfiguration {
    public RedisLettuceFindHashNoSaveApp() {
        super("redislettucefindhashnosave");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLettuceFindHashNoSaveApp.class, args);
    }

}
