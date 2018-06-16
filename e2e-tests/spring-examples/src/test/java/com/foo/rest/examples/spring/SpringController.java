package com.foo.rest.examples.spring;

import org.evomaster.clientJava.controller.EmbeddedSutController;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public abstract class SpringController extends EmbeddedSutController {

    protected ConfigurableApplicationContext ctx;
    protected final Class<?> applicationClass;



    protected SpringController(Class<?> applicationClass){
       this.applicationClass = applicationClass;
       super.setControllerPort(0);
    }


    @Override
    public String startSut() {

        ctx = SpringApplication.run(applicationClass, "--server.port=0");


        return "http://localhost:"+getSutPort();
    }

    protected int getSutPort(){
        return (Integer)((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }


    @Override
    public boolean isSutRunning() {
        return ctx!=null && ctx.isRunning();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.";
    }

    @Override
    public void resetStateOfSUT() {
        //nothing to do
    }

    @Override
    public String getUrlOfSwaggerJSON() {
        return "http://localhost:"+getSutPort()+"/v2/api-docs";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public String getDatabaseDriverName() {
        return null;
    }

    @Override
    public List<String> getEndpointsToSkip(){
        return null;
    }
}
