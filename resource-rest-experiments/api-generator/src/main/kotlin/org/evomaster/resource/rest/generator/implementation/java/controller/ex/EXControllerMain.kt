package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-11
 */
class EXControllerMain(val exClazz :String, val jarName : String, val csName:String, val rootProject : String) : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf("args" to "String[]")

    override fun getBody(): List<String> = listOf(
            """
                int controllerPort = 40100;
                if (args.length > 0) {
                  controllerPort = Integer.parseInt(args[0]);
                }
                int sutPort = 12345;
                if (args.length > 1) {
                  sutPort = Integer.parseInt(args[1]);
                }
                String jarLocation = "$rootProject$csName/target";
                if (args.length > 2) {
                  jarLocation = args[2];
                }
                if(! jarLocation.endsWith(".jar")) {
                  jarLocation += "/$jarName.jar";
                }
            
                int timeoutSeconds = 120;
                if(args.length > 3){
                  timeoutSeconds = Integer.parseInt(args[3]);
                }
            
                ExternalEvoMasterController controller =
                        new $exClazz(controllerPort, jarLocation, sutPort, timeoutSeconds);
                InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
            
                starter.start();
            """.trimIndent()
    )

    override fun getName(): String = "main"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun isStatic(): Boolean {
        return true
    }
}