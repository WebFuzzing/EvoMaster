package com.foo.web.examples.spring.dropdown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class DropDownWebApplication {

    public static void main(String[] args){
        SpringApplication.run(DropDownWebApplication.class);
    }


}
