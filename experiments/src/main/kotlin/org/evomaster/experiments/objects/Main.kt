package org.evomaster.experiments.objects

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.search.gene.MapGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.StringGene
import java.lang.Integer.max
import java.util.*

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

            val sampler = setStuffUp().getInstance(ObjRestSampler::class.java)

            //BMR: debugginPrint uses .getName() which returns {variable} etc...
            //BMR: debugginPrintProcessed uses .toString(), and seems to return the {variable} values already processed.

            val models = sampler.getModelCluster()

            val available = sampler.seeAvailableActions()

            println("Overview of Models:")
            models.forEach{
                println("${it.key} => ${it.value.getValueAsPrintableString()}")
            }


            //nameMatchExperiments(models)


            /*
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
            }*/

            println("===============================================>")

            println("Available actions:")
            available.forEach{
                println("${it.getName()}")
                it.seeGenes().forEach {
                    println(" -> ${it.getVariableName()}")
                    val likely = likelyhoods(it.getVariableName(), models)
                    println("Likelyhoods: ${likely}")
                    (0..10).forEach{k ->
                        val sel = pickWithProbability(likely)
                        val selObj = sampler.getObjectFromTemplate(sel)
                        println("Selected -> ${selObj.name} => ${selObj.getValueAsPrintableString()}")
                        if(!selObj.fields.isEmpty()){
                            println("Picked field -> ${selObj.fields.random().getValueAsPrintableString()}")
                        }
                    }
                }
                println(" --- ")
            }

            println("===============================================>")

            //randomObjectExperiments(sampler, 10)

            println("===============================================>")

            //swaggerishObjectExperiments(sampler, 50)


            //var smarts = sampler.getRandomObjIndividual()
            //println("Let's try to be smart again: ${smarts.debugginPrintProcessed()}")
            // TODO: Smart need more work
        }

        fun printHeader() {
            println("Let's start playing about")
        }

        fun setStuffUp () : Injector{

            val base = BaseModule()
            //BMR: just to get it running (and to have a clue how to add the selector to it
            val problemType = EMConfig.ProblemType.REST

            val problemModule = when (problemType) {
                EMConfig.ProblemType.REST -> ObjModule()
                //this should never happen, unless we add new type and forget to add it here
                else -> throw IllegalStateException("Unrecognized problem type: $problemType")
            }
            val injector = LifecycleInjector.builder()
                    .withModules(base, problemModule)
                    .build()
                    .createInjector()

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

        fun likelyhoods(parameter: String, candidates: MutableMap<String, ObjectGene>): MutableMap<String, Float>{

            var result = mutableMapOf<String, Float>()
            var sum : Float = 0.toFloat()
            candidates.forEach { k, v ->
                val temp = lcs(parameter, k).length.toFloat()/max(parameter.length, k.length).toFloat()
                result[k] = temp
                sum += temp
            }
            result.forEach { k, u ->
                result[k] = u/sum
            }

            return result
        }

        fun lcs(a: String, b: String): String {
            if (a.length > b.length) return lcs(b, a)
            var res = ""
            for (ai in 0 until a.length) {
                for (len in a.length - ai downTo 1) {
                    for (bi in 0 until b.length - len) {
                        if (a.regionMatches(ai, b, bi,len) && len > res.length) {
                            res = a.substring(ai, ai + len)
                        }
                    }
                }
            }
            return res
        }

        fun pickWithProbability(map: MutableMap<String, Float>): String{
            val randFl = Random().nextFloat()
            var temp = 0.toFloat()
            var found = map.keys.first()

            for((k, v) in map){
                if(randFl <= (v + temp)){
                    found = k
                    break
                }
                temp += v
            }
            return found
        }

        fun nameMatchExperiments(models: MutableMap<String, ObjectGene>){
            println("Name match experiments:")
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
        }

        fun randomObjectExperiments(sampler: ObjRestSampler, numberOfObjects: Int = 10){
            for (i in 0..numberOfObjects) {
                var randomobject = sampler.getRandomObject()
                println("Pseudo-random object: ${randomobject.getValueAsPrintableString()}")
            }
        }

        fun swaggerishObjectExperiments(sampler: ObjRestSampler, numberOfObjects: Int = 25){
            for (i in 0..numberOfObjects) {
                var swaggerishRandomobject = sampler.getRandomSwaggerObject()
                println("Swagger object: ${swaggerishRandomobject.name} =>> ${swaggerishRandomobject.getValueAsPrintableString()}")
            }
        }

    }

}