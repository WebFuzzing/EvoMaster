package org.evomaster.resource.rest.generator.implementation.java.em

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-11
 */
class EMControllerMain(val emClazz : String) : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf("args" to "String[]")

    override fun getBody(): List<String> = listOf(
            """
                int port = 40100;
                if (args.length > 0) {
                  port = Integer.parseInt(args[0]);
                }
            
                controller = new $emClazz(port);

                embeddedStarter = new InstrumentedSutStarter(controller);
                embeddedStarter.start();
            
                controllerPort = embeddedStarter.getControllerServerPort();
            
                remoteController = new RemoteController("localhost", controllerPort, true);
                boolean started = remoteController.startSUT();
            
                SutInfoDto dto = remoteController.getSutInfo();
            
                baseUrlOfSut = dto.baseUrlOfSUT;
            
                System.out.println("Remote controller running on port " + controllerPort);
                System.out.println("SUT listening on " + baseUrlOfSut);
            """.trimIndent()
    )

    override fun getName(): String = "main"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun isStatic(): Boolean {
        return true
    }
}