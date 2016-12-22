package org.evomaster.core

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module


/**
 * This will be the entry point of the tool when run from command line
 */
class Main{
    companion object{

        @JvmStatic
        fun main(args : Array<String>) {

            val parser = EMConfig.getOptionParser()
            val options = parser.parse(*args)

            //TODO check problem type

            val injector: Injector = Guice.createInjector(* arrayOf<Module>(
                   BaseModule()))

            //TODO update seed

            //TODO check algorithm
        }

    }
}



