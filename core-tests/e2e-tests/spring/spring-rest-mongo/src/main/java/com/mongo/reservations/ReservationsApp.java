package com.mongo.reservations;

import com.mongo.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class ReservationsApp extends SwaggerConfiguration {
    public ReservationsApp() {
        super("reservations");
    }

    public static void main(String[] args) {
        SpringApplication.run(ReservationsApp.class, args);
    }

}