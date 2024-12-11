package org.evomaster.core

import org.evomaster.core.AnsiColor.Companion.inBlue
import org.evomaster.core.AnsiColor.Companion.inRed
import org.evomaster.core.logging.LoggingUtil
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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

            //This needs to be kept in sync with core/pom.xml -> maven-assembly-plugin
            val command = "--add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

            LoggingUtil.getInfoLogger().error(inRed("It looks like you are running EvoMaster with JDK 17+," +
                    " but did not setup '--add-opens'." +
                    " Unfortunately, Java has broken a lot of backward compatibility in its recent versions." +
                    " You should use the EvoMaster OS installers (e.g., .msi and .dmg files)." +
                    " If you use the JAR file directly," +
                    " --add-opens are now set directly in the manifest file." +
                    " But this is not the case if you run EvoMaster directly from inside an IDE." +
                    " You would need to set JVM options manually in the IDE" +
                    "  (or in the JDK_JAVA_OPTIONS environment variable):\n") +
                    inBlue("$command"))

            return false
        }
    }

    fun getJDKVersion() : Int {
        //from https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
        var version = System.getProperty("java.version")
        if (version.startsWith("1.")) {
            version = version.substring(2, 3)
        } else {
            val dot = version.indexOf(".")
            if (dot != -1) {
                version = version.substring(0, dot)
            }
        }
        return version.toInt()
    }


    /**
     * This is a shitshow.
     * JDK has ancient implementation of HTTP, not supporting PATCH method... yep, WTF!?!?!?
     * Frameworks like Jersey fixes things at runtime via reflection.
     * There are --add-opens challenges for JDK 16+, but those can be handled.
     * Somehow, though, this fails on GA, even for JDK 8... no idea what the heck is going on :(
     * So we force a hack here
     *
     * https://github.com/eclipse-ee4j/jersey/issues/4825
     */
    fun fixPatchMethod(){

        //val methods = arrayOf("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH");

        val field = HttpURLConnection::class.java.getDeclaredField("methods")
        field.isAccessible = true


        val original = field.get(null) as Array<String>

        if(original.any { it == "PATCH" }){
            return //nothing to do, already fixed
        }

        val index = original.indexOfFirst { it == "TRACE" }
        // I feel so dirty... fuck you JDK 17+
        original[index] = "PATCH"

        //Unfortunately, this does not work with recent JDK versions :(
//        val modifiersField = Field::class.java.getDeclaredField("modifiers")
//        modifiersField.isAccessible = true
//        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
//        field.set(null, methods)
    }

}