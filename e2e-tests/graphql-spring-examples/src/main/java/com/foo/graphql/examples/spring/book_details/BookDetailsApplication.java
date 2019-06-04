package com.foo.graphql.examples.spring.book_details;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class BookDetailsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookDetailsApplication.class, args);
    }

}