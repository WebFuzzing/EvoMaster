package org.evomaster.casestudy.rest.runner;

public class RunnerUtils {

    public static void initInstrumentationJarLocation(){

        /*
            Needed for when run from IDE
         */

        String version = "0.0.2";
        System.setProperty("evomaster.instrumentation.jar.path",
                "../../client-java/instrumentation/target/evomaster-client-java-instrumentation-"+version+"-SNAPSHOT.jar");
    }
}
