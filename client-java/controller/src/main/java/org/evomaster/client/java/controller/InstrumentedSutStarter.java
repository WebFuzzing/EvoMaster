package org.evomaster.client.java.controller;

import com.ea.agentloader.AgentLoader;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.InstrumentingAgent;

/**
 * Class responsible to handle the SutController in a way
 * in which bytecode instrumentation is activated.
 * Note: instrumentation is needed when generating tests
 * with EvoMaster, but not when we run the generated tests.
 */
public class InstrumentedSutStarter {

    static {
        /*
            Force loading of Agent here, just to make sure it is called only once

            note: just passing a valid package at initialization, but that
            ll be modified later if needed.
         */
        /*
            This was not needed for external driver... it became a issue for JDK 9+, where agent
            are not allowed by default. so now we only do for embedded
         */
        //AgentLoader.loadAgentClass(InstrumentingAgent.class.getName(), "foobar_packagenameshouldnotexist.");
    }


    /**
     *   Annoying settings needed for JDK 17 :(
     *
     *   Update docs/jdks.md if this changes
     */
    public static final String JDK_17_JVM_OPTIONS = "--add-opens java.base/java.util.regex=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED";

    private static boolean alreadyLoaded = false;

    private final SutController sutController;

    public static void loadAgent(){
        if(! alreadyLoaded){
            alreadyLoaded = true;
            try {
                AgentLoader.loadAgentClass(InstrumentingAgent.class.getName(), "foobar_packagenameshouldnotexist.");
            } catch (Exception e){
                throw new RuntimeException(
                        "\nFailed to apply bytecode instrumentation with JavaAgent." +
                        "\nIf you are using JDK 11 or above, are you sure you added the following JVM option?" +
                        "\n -Djdk.attach.allowAttachSelf=true " +
                        "\nAlso, if you are using JDK 17 or above, you also need the following:" +
                        "\n"+JDK_17_JVM_OPTIONS+
                        "\nThis can also be set globally with the JDK_JAVA_OPTIONS environment variable."+
                        "\nSee documentation at https://github.com/EMResearch/EvoMaster/blob/master/docs/jdks.md", e);
            }
        }
    }

    public InstrumentedSutStarter(SutController sutController) {

        //need to be called before ClassesToExclude is loaded into memory
        String toSkip = sutController.packagesToSkipInstrumentation();
        if(toSkip != null && !toSkip.isEmpty()) {
            System.setProperty(Constants.PROP_SKIP_CLASSES, toSkip);
        }

        this.sutController = sutController;

        if (sutController instanceof EmbeddedSutController) {
            loadAgent();
            InstrumentingAgent.changePackagesToInstrument(sutController.getPackagePrefixesToCover());

        } else if(sutController instanceof ExternalSutController){
            ((ExternalSutController)sutController).setInstrumentation(true);
            /*
                Reducing amount of logging from Jetty.
                Note: this is not done for embedded one, as that might conflict with
                the SUT if the SUT is using Jetty.
             */
            System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
            System.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        } else {
            throw new IllegalArgumentException("Invalid SUT controller type");
        }
    }

    public boolean start() {
       // StandardOutputTracker.setTracker(true, sutController);
        return sutController.startTheControllerServer();
    }

    public boolean stop() {
       // StandardOutputTracker.setTracker(false, null);
        return sutController.stopTheControllerServer();
    }

    public int getControllerServerPort() {
        return sutController.getControllerServerPort();
    }

}
