package com.foo.rest.examples.spring.regexdate;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.time.LocalDate;

/**
 * Created by arcuri82 on 11-Jun-19.
 */
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@RestController
public class RegexDateApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(RegexDateApplication.class, args);
    }


    @GetMapping(path = "/api/{date:\\d{4}-\\d{1,2}-\\d{1,2}}-{seq:\\d+}")
    public String get(
            @PathVariable("date") String date,
            @PathVariable("seq") String seq
    ){
        return LocalDate.parse(date).toString() + "-" + Integer.parseInt(seq);
    }
}
