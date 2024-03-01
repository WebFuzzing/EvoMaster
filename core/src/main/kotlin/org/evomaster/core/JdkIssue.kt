package org.evomaster.core

import org.evomaster.core.AnsiColor.Companion.inBlue
import org.evomaster.core.AnsiColor.Companion.inRed
import org.evomaster.core.logging.LoggingUtil
import java.net.HttpURLConnection

object JdkIssue {


    fun checkAddOpens() : Boolean{

        try {
            var field = HttpURLConnection::class.java.getDeclaredField("method")
            field.isAccessible = true

            field = HashMap::class.java.getDeclaredField("threshold")
            field.isAccessible = true

            return true
        }catch (e: Exception){

            //This needs to be kept in sync with makeExecutable.sh
            val command = "--add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

            LoggingUtil.getInfoLogger().error(inRed("It looks like you are running EvoMaster with JDK 17+," +
                    " but did not setup '--add-opens'." +
                    " Unfortunately, Java has broken a lot of backward compatibility in its recent versions." +
                    " You should use the EvoMaster OS installers (e.g., .msi and .dmg files)." +
                    " If you want to use the JAR file directly, you need to either upgrade to JDK 8 or 11, or" +
                    " setup --add-opens JVM option manually on the command-line" +
                    "  (or in the JDK_JAVA_OPTIONS environment variable):\n") +
                    inBlue("$command"))

            return false
        }

    }
}