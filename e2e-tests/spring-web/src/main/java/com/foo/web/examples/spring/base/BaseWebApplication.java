package com.foo.web.examples.spring.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class BaseWebApplication {

    /*
        All different web apps that only use static assets can use this class.
        apps are distinguished based on folders in "resources/static"
     */

    public static void main(String[] args){
        SpringApplication.run(BaseWebApplication.class);
    }
}
