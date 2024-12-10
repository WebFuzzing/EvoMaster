package org.evomaster.core.logging

import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TestLoggingUtil {

    companion object{

        /**
         * Run lambda within a deterministic version of logback.xml (e.g., no time-stamps),
         * and return the generated logs
         */
        fun runWithDeterministicLogger(lambda: () -> Unit) : String{

            val latestOut = System.out

            //create an in-memory buffer to write to
            val byteStream = ByteArrayOutputStream()
            val outStream = PrintStream(byteStream)

        //    if (latestOut is org.evomaster.sql.internal.WrappedPrintStream){
                /*
                    we can also manipulate the current [latestOut], i.e., set [outStream] as printStream of
                    the current StandardOutputTracker, but it might cause some problems when setting its status back.
                    therefore, here, we create a new PrintStream which keeps the same setting with the current one.
                 */
           //     System.setOut(latestOut.copyWithRestPrintStream(outStream))
          //  }else{
                System.setOut(outStream)
         //   }

            LoggingUtil.changeLogbackFile("logback_for_determinism_check.xml")

            try {
                lambda()
            } finally {
                //before returning the logs, restore the default settings
                LoggingUtil.changeLogbackFile("logback.xml")
                System.setOut(latestOut)
            }

            val logs = byteStream.toString()

            //as done by a separated thread, their ordering in the logs is not guaranteed, so we skip them
            val filtered = logs.split("\n")
                //.filter { ! it.startsWith(P6SpyFormatter.PREFIX) }
                .joinToString("\n")

            return filtered
        }
    }
}