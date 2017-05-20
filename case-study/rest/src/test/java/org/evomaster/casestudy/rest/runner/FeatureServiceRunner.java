package org.evomaster.casestudy.rest.runner;

import org.evomaster.clientJava.controller.ExternalSutController;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;

import java.util.List;

public class FeatureServiceRunner extends ExternalSutController {

    public static void main(String[] args) {

        int controllerPort = 40100;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = 12345;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "jars";
        if(args.length > 2){
            jarLocation = args[2];
        }
        jarLocation += "/features-service.jar";

        FeatureServiceRunner controller =
                new FeatureServiceRunner(controllerPort, jarLocation, sutPort);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int sutPort;
    private final String jarLocation;

    public FeatureServiceRunner(){
        this(40100, "jars/features-service.jar", 50100);
    }

    public FeatureServiceRunner(int controllerPort, String jarLocation, int sutPort) {
        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        setControllerPort(controllerPort);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{"--server.port="+sutPort};
    }

    public String[] getJVMParameters() {
        return new String[0];
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:"+sutPort;
    }

    @Override
    public String getPathToExecutableJar() {
        return jarLocation;
    }

    @Override
    public String getLogMessageOfInitializedServer() {
        return "Started Application in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return 60;
    }

    public void preStart() {

    }

    public void postStop() {

    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.javiermf.features.";
    }


    public void resetStateOfSUT() {
        //TODO
    }

    @Override
    public String getUrlOfSwaggerJSON() {
        return getBaseURL() +"/swagger.json";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }
}
