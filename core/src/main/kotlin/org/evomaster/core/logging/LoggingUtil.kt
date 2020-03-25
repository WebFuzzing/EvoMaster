package org.evomaster.core.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream





/**
    See "logback.xml" under the resources folder
 */
class LoggingUtil {

    companion object{

        /**
         * Logger used to provide expected info to the user
         */
        fun getInfoLogger(): Logger =
                LoggerFactory.getLogger("info_logger") ?:
                        throw IllegalStateException("Failed to init logger")


        /**
         *  A WARN log that can be printed only once.
         *  If called twice (or more), such calls are ignored
         */
        fun uniqueWarn(log: Logger, msg: String){
            log.warn(UniqueTurboFilter.UNIQUE_MARKER, msg)
        }

        /**
         *  A WARN log that can be printed only once.
         *  If called twice (or more), such calls are ignored
         */
        fun uniqueWarn(log: Logger, msg: String, arg: Any){
            log.warn(UniqueTurboFilter.UNIQUE_MARKER, msg, arg)
        }


        /**
         *   Only needed for testing/debugging
         */
        fun changeLogbackFile(resourceFilePath: String): Boolean {

            require(resourceFilePath.endsWith(".xml")) { "Logback file name does not terminate with '.xml'" }

            val context: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            try {
                val configurator = JoranConfigurator()
                configurator.context = context

                var f: InputStream? = null
                f = if (LoggingUtil::class.java.classLoader != null) {
                    LoggingUtil::class.java.classLoader.getResourceAsStream(resourceFilePath)
                } else {
                    // If the classloader is null, then that could mean the class was loaded
                    // with the bootstrap classloader, so let's try that as well
                    ClassLoader.getSystemClassLoader().getResourceAsStream(resourceFilePath)
                }
                if (f == null) {
                    val msg = "$resourceFilePath not found on classpath"
                    System.err.println(msg)
                    return false
                } else {
                    context.reset()
                    configurator.doConfigure(f)
                }
            } catch (je: JoranException) {
                return false
            }
            return true
        }


        /**
         * Run lambda within a deterministic version of logback.xml (e.g., no time-stamps),
         * and return the generated logs
         */
        fun runWithDeterministicLogger(lambda: () -> Unit) : String{

            //create an in-memory buffer to write to
            val byteStream = ByteArrayOutputStream()
            val outStream = PrintStream(byteStream)
            val latestOut = System.out
            System.setOut(outStream)

            changeLogbackFile("logback_for_determinism_check.xml")

            try {
                lambda()
            } finally {
                //before returning the logs, restore the default settings
                changeLogbackFile("logback.xml")
                System.setOut(latestOut)
            }

            val logs = byteStream.toString()

            return logs
        }
    }
}