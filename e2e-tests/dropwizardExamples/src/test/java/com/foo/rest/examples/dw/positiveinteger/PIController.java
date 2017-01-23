package com.foo.rest.examples.dw.positiveinteger;

import org.evomaster.clientJava.controller.RestController;

public class PIController extends RestController{

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
}
