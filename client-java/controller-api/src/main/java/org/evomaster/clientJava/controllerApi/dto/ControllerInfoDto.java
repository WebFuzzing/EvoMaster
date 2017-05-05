package org.evomaster.clientJava.controllerApi.dto;

public class ControllerInfoDto {

    /**
     * The full qualifying name of the controller.
     * This will be needed when tests are generated, as those
     * will instantiate and start the controller directly
     */
    public String fullName;


    /**
     * Whether the system under test is running with instrumentation
     * to collect data about its execution
     */
    public Boolean isInstrumentationOn;
}
