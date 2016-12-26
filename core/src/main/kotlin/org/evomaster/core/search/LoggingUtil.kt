package org.evomaster.core.search

import org.slf4j.LoggerFactory

/**
    See "logback.xml" under the resources folder
 */
class LoggingUtil {

    companion object{

        /**
         * Logger used to provide expected info to the user
         */
        fun getInfoLogger() = LoggerFactory.getLogger("info_logger");
    }
}