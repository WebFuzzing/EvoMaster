package org.evomaster.core

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.LoggingUtil
import org.slf4j.LoggerFactory


/**
 * This will be the entry point of the tool when run from command line
 */
class Main{
    companion object{

        @JvmStatic
        fun main(args : Array<String>) {

            try {
                val parser = EMConfig.getOptionParser()
                val options = parser.parse(*args)

                //TODO check problem type

                val injector: Injector = LifecycleInjector.builder()
                        .withModules(* arrayOf<Module>(BaseModule()))
                        .build().createInjector()

                //TODO update EMConfig
                //TODO update seed

                //TODO check algorithm
            } catch (e: Exception){
                LoggingUtil.getInfoLogger()
                        .error("ERROR: EvoMaster process terminated abruptly. Message: "+e.message, e)
            }
        }

    }
}



