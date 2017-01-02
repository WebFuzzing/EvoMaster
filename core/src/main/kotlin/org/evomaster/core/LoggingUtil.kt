package org.evomaster.core

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
    }
}