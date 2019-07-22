package org.evomaster.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    }
}