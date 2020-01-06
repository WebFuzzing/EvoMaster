package org.evomaster.resource.rest.generator.implementation.java.controller.em

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
            
                EmbeddedEvoMasterController controller = new $emClazz(port);
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