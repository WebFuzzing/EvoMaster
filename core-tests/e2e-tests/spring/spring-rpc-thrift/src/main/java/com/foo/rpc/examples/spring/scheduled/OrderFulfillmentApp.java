package com.foo.rpc.examples.spring.scheduled;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class OrderFulfillmentApp {

    public static void main(String[] args) {
        SpringApplication.run(OrderFulfillmentApp.class, args);
    }

    @Bean
    public TProtocolFactory tProtocolFactory() {
        return new TBinaryProtocol.Factory();
    }

    @Bean
    public ServletRegistrationBean orderFulfillmentServlet(TProtocolFactory protocolFactory, OrderFulfillmentServiceImp service) {
        TServlet tServlet = new TServlet(new OrderFulfillmentService.Processor<>(service), protocolFactory);
        return new ServletRegistrationBean(tServlet, "/order-fulfillment");
    }
}
