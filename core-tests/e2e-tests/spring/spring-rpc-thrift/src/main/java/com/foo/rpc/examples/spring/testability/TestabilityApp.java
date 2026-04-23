package com.foo.rpc.examples.spring.testability;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class TestabilityApp {

    public static void main(String[] args) {
        SpringApplication.run(TestabilityApp.class, args);
    }

    @Bean
    public TProtocolFactory tProtocolFactory() {
        return new TBinaryProtocol.Factory();
    }

    @Bean
    public ServletRegistrationBean dbBaseServlet(TProtocolFactory protocolFactory, TestabilityServiceImp service) {
        TServlet tServlet =  new TServlet(new TestabilityService.Processor<>(service), protocolFactory);
        return new ServletRegistrationBean(tServlet, "/testability");
    }

}
