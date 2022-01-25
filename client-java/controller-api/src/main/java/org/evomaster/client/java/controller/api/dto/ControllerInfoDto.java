package org.evomaster.client.java.controller.api.dto;

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

    /**
     * In some cases, like for External drivers for JVM, we might need the full path of
     * where the executable (or jar files) is located.
     * As this info might be parametric in the driver (it is for all SUTs in EMB), or given
     * as relative path (to be able to run on different machines), we need to collect the full
     * absolute path. This will be used in the generated tests.
     */
    public String executableFullPath;
}
