package org.evomaster.core.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker
import org.evomaster.client.java.controller.internal.db.WrappedPrintStream
import org.evomaster.client.java.databasespy.P6SpyFormatter
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

            val latestOut = System.out

            //create an in-memory buffer to write to
            val byteStream = ByteArrayOutputStream()
            val outStream = PrintStream(byteStream)

            if (latestOut is WrappedPrintStream){
                /*
                    we can also manipulate the current [latestOut], i.e., set [outStream] as printStream of
                    the current StandardOutputTracker, but it might cause some problems when setting its status back.
                    therefore, here, we create a new PrintStream which keeps the same setting with the current one.
                 */
                System.setOut(latestOut.copyWithRestPrintStream(outStream))
            }else{
                System.setOut(outStream)
            }

            changeLogbackFile("logback_for_determinism_check.xml")

            try {
                lambda()
            } finally {
                //before returning the logs, restore the default settings
                changeLogbackFile("logback.xml")
                System.setOut(latestOut)
            }

            val logs = byteStream.toString()

            //as done by a separated thread, their ordering in the logs is not guaranteed, so we skip them
            val filtered = logs.split("\n")
                    .filter { ! it.startsWith(P6SpyFormatter.PREFIX) }
                    .joinToString("\n")

            return filtered
        }
    }
}


