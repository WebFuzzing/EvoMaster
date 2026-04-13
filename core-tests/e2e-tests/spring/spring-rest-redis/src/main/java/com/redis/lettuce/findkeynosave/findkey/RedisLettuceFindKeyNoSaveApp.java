package com.redis.lettuce.findkeynosave.findkey;

import com.redis.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RedisLettuceFindKeyNoSaveApp extends SwaggerConfiguration {
    public RedisLettuceFindKeyNoSaveApp() {
        super("redislettucefindkeynosave");
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLettuceFindKeyNoSaveApp.class, args);
    }

}
