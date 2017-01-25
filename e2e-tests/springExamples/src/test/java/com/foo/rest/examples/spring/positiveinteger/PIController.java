package com.foo.rest.examples.spring.positiveinteger;

import org.evomaster.clientJava.controller.RestController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.util.Map;

public class PIController  extends RestController {

    private ConfigurableApplicationContext ctx;

    @Override
    public int getControllerPort(){
        return 0;
    }

    @Override
    public String startSut() {

        //TODO is this blocking until initialized???
        ctx = SpringApplication.run(PIApplication.class,
                new String[]{"--server.port=0"});


        return "http://localhost:"+getSutPort();
    }

    protected int getSutPort(){
        return (Integer)((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }


    @Override
    public String startInstrumentedSut() {
        return startSut();
    }

    @Override
    public boolean isSutRunning() {
        return ctx!=null && ctx.isRunning(); //TODO check if correct
    }

    @Override
    public void stopSut() {
        ctx.stop();
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
        return "TODO";
    }
}
