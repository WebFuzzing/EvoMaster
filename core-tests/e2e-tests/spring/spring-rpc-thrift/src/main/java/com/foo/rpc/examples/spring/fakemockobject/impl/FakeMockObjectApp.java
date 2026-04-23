package com.foo.rpc.examples.spring.fakemockobject.impl;

import com.foo.rpc.examples.spring.fakemockobject.generated.FakeMockObjectService;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.net.URLClassLoader;

@Configuration
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class FakeMockObjectApp {

    /*
        enable this later for extracting specified data schema
     */
//    private static final String JAR_TO_LOAD = "jars/Fake-1.0-SNAPSHOT.jar";

    public static void main(String[] args) {
        SpringApplication.run(FakeMockObjectApp.class, args);
    }

//    private static void loadClass(){
//        // load jar
//        try {
//            ClassLoader loader = FakeMockObjectApp.class.getClassLoader();
//            URL jar = loader.getResource(JAR_TO_LOAD);
//            URLClassLoader child = new URLClassLoader(
//                new URL[] {jar},
//                loader
//            );
//            Class.forName("fake.db.GetDbData", true, child);
//        } catch (Throwable e) {
//            throw new RuntimeException("fail to load jar", e);
//        }
//    }

    @Bean
    public TProtocolFactory tProtocolFactory() {
        return new TBinaryProtocol.Factory();
    }

    @Bean
    public ServletRegistrationBean customizationServlet(TProtocolFactory protocolFactory, FakeMockObjectServiceImpl service) {
        TServlet tServlet =  new TServlet(new FakeMockObjectService.Processor<>(service), protocolFactory);
        return new ServletRegistrationBean(tServlet, "/fakemockobject");
    }
}
