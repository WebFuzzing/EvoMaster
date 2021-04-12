package com.foo.rest.examples.spring.openapi.v3.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class StatisticsApplication {

    public static void main(String[] args) { SpringApplication.run(StatisticsApplication.class, args); }

}
