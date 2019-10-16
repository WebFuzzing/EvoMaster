package com.foo.rest.examples.spring.tcpport;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@EnableSwagger2
@RestController
@RequestMapping
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class TcpPortApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(TcpPortApplication.class, args);
    }


    private static final Set<Integer> ports = new HashSet<>();


    @GetMapping(path = "/api/tcpPort")
    public Set<Integer> get(HttpServletRequest request){

        String connection = request.getHeader("connection");
        if( ! connection.equalsIgnoreCase("keep-alive")){
           throw new IllegalArgumentException("Should always have keep-alive");
        }

        int p = request.getRemotePort();
        ports.add(p);

        return ports;
    }
}
