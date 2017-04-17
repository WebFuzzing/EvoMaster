package com.foo.rest.examples.dw.positiveinteger;

import org.evomaster.clientJava.controller.SutController;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;

import java.util.List;

public class PIController extends SutController {

    private PIApplication application;

    @Override
    public int getControllerPort(){
        return 0;
    }

    @Override
    public String startSut() {

        application = new PIApplication(0);
        try {
            application.run("server");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
        }

        while(! application.getJettyServer().isStarted()){
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }

        return "http://localhost:"+application.getJettyPort();
    }

    @Override
    public String startInstrumentedSut() {
        return startSut();
    }

    @Override
    public boolean isSutRunning() {
        if(application == null){
            return false;
        }

        return application.getJettyServer().isRunning();
    }

    @Override
    public void stopSut() {
        if(application != null) {
            try {
                application.getJettyServer().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        return "http://localhost:"+application.getJettyPort()+"/api/swagger.json";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }
}
