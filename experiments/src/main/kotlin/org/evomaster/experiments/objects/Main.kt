package org.evomaster.experiments.objects

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.search.gene.MapGene
import org.evomaster.core.search.gene.StringGene

class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

           // Main.testMapPrint()
            Main.base()
//            infeasible()
        }

        private fun base () {
            printHeader()

            //println("Injector: ${inj}")

            val sampler = setStuffUp().getInstance(ObjRestSampler::class.java)

            //println("And the sampler ref is:... TA DAAAAA: ${sampler}")

            //val action = sampler.sampleAtRandom()

            //println("I took an action, me: ${action.debugginPrint()} ")
            //BMR: debugginPrint uses .getName() which returns {variable} etc...
            //println("In practice it would look like: ${action.debugginPrintProcessed()} ")
            //BMR: debugginPrintProcessed uses .toString(), and seems to return the {variable} values already processed.
            //TODO figure out why the model overview is printed 3 times

            val models = sampler.getModelCluster()

            val available = sampler.seeAvailableActions()
            //models["Organization"].fields.forEach{ t -> t.name.contains("Id"); println("${t.name} => ${t.getValueAsPrintableString()}") }

            models.forEach{(k, v) -> v.fields.forEach {
                if(it.name.equals("id", ignoreCase = true) || it.name.equals("name", ignoreCase = true)){
                    println("Exact Match Model: $k => ${it.name} =>>> ${it.getValueAsPrintableString()}")
                }
                else{
                    if (it.name.contains("id", ignoreCase = true) || it.name.contains("name", ignoreCase = true)){
                        println("Partial Match Model: $k => ${it.name} =>>> ${it.getValueAsPrintableString()}")
                    }
                }
            }}

            for (ac in available){
                var genes = ac.seeGenes()
                for (g in genes){
                    if (g.getVariableName().contains("Id")){
                        println("(Id): Action ${ac.getName()}; Gene ${g.getVariableName()}")
                    }
                    if (g.getVariableName().contains("Name")){
                        println("(Name): Action ${ac.getName()}; Gene ${g.getVariableName()}")
                    }
                }
            }

            println("Available actions: $available")

            println("===============================================>")

            for (i in 0..5) {
                var randomobject = sampler.getRandomObject()
                println("I have a pseudo-random object: ${randomobject.getValueAsPrintableString()}")
            }


            println("===============================================>")

            for (i in 0..25) {
                var swaggerishRandomobject = sampler.getRandomObjectSwaggerish()
                println("I have a swaggerish-random object: ${swaggerishRandomobject.name} =>> ${swaggerishRandomobject.getValueAsPrintableString()}")
            }


            //var smarts = sampler.getRandomObjIndividual()
            //println("Let's try to be smart again: ${smarts.debugginPrintProcessed()}")
            // TODO: Smart need more work
        }

        fun printHeader() {
            println("Let's start playing about")
        }

        fun setStuffUp () : Injector{
            val injector = LifecycleInjector.builder()
                    .withModules(ObjModule(), BaseModule(arrayOf("--showProgress=false")))
                    .build().createInjector()

            return injector

        }

        fun testMapPrint() {

            println("Quick test of the map print ")
            val s1 = StringGene("string_1")
            val s2 = StringGene("string_2")

            println("String 1 is: ${s1.getValueAsPrintableString()}")
            println("String 2 is: ${s2.getValueAsPrintableString()}")

            var map = MapGene<StringGene>("PrintableMap", StringGene("map"), 7, mutableListOf(s1, s2))
            var mapstring = map.getValueAsPrintableString()
            println("PrintableMap is =>  $mapstring")
            assert(mapstring.contains(s1.getValueAsPrintableString(), ignoreCase = true) &&
                    mapstring.contains(s2.getValueAsPrintableString(), ignoreCase = true))
        }


    }

}