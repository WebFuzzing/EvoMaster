package com.foo.rest.examples.spring.base64;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Base64;

/**
 *
 */
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@RestController
@RequestMapping(path = "/api/base64/decode")
public class Base64DecodeApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Base64DecodeApplication.class, args);
    }
    @GetMapping
    public ResponseEntity<?> get(String src){
        byte[] output = Base64.getDecoder().decode(src);
        if (output.length>4) {
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
