package org.evomaster.experiments.objects

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.TestSuiteWriter
import org.evomaster.core.problem.rest.ObjIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.experiments.objects.service.ObjModule
import org.evomaster.experiments.objects.service.ObjRestSampler
import java.lang.Integer.max
import java.util.*
import javax.swing.text.html.Option

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

            val injector = setStuffUp()
            val sampler = injector.getInstance(ObjRestSampler::class.java)

            val key = Key.get( object : TypeLiteral<MioAlgorithm<ObjIndividual>>() {})

            val imp = injector.getInstance(key)

            //BMR: debugginPrint uses .getName() which returns {variable} etc...
            //BMR: debugginPrintProcessed uses .toString(), and seems to return the {variable} values already processed.

            val models = sampler.getModelCluster()
                    .filter { (_, m) -> !(m.fields.isEmpty()) } as MutableMap
            // TODO: this needs to be thought out better in future, but let's trim the model set to remove empties.


            if (models.isEmpty()){
                val col = mutableListOf(StringGene("default"))
                val someCol = ObjectGene(
                        "Default",
                        col
                )
                models["Default"] = someCol
            }

            val available = sampler.seeAvailableActions()

            println("Overview of Models:")
            models.forEach{
                println("${it.key} => ${it.value.getValueAsPrintableString()}")
            }


            //nameMatchExperiments(models)



            println("===============================================>")

            println("Available callActions:")
            available.forEach{it ->
                println("===============================================>")
                //val action = it.copy() as ObjRestCallAction

                val action = sampler.sampleRandomObjCallAction(0.0)
                println(action.getName())


/*
                action.seeGenes().forEach {g ->

                    println(" --- ")

                    val restrictedModels = mutableMapOf<String, ObjectGene>()
                    models.forEach{ k, model ->
                        val fields = model.fields.filter { field ->
                            when (g::class) {
                                DisruptiveGene::class -> (field as OptionalGene).gene::class === (g as DisruptiveGene<*>).gene::class
                                OptionalGene::class -> (field as OptionalGene).gene::class === (g as OptionalGene).gene::class
                                else -> {
                                    false
                                }
                            }
                        }

                        restrictedModels[k] = ObjectGene(model.name, fields)

                    }


                    val likely = likelyhoodsExtended(g.getVariableName(), restrictedModels).toList().sortedBy { (_, value) -> -value}.toMap()

                    println("Likelyhoods: ${likely}")


                    println(" -> ${g.getVariableName()}")
                    val sel = pickWithProbability(likely as MutableMap<Pair<String, String>, Float>)

                    println("Selected: ${sel.first} -> ${sel.second}")

                    val selectedGene = models.get(sel.first)?.fields?.filter { g -> (g as OptionalGene).name === sel.second }?.single() as OptionalGene

                    println(selectedGene.name)



                }
*/
                println("Action after selection: ${action.resolvedPath()}")

            }

            // TODO: when picking an object, make sure it has fields to match the data required by the action.
            // E.g. do not pick an object without any numerical values, if an Int id is required.

            println("===============================================>")

            //randomObjectExperiments(sampler, 10)


            println("===============================================>")
            //swaggerishObjectExperiments(sampler, 50)

            val smarts = sampler.smartSample()
            println("One more try on smart: ${smarts.debugginPrint()} => ${smarts.debugginPrintProcessed()}")
            //var smarts = sampler.getRandomObjIndividual()
            //println("Let's try to be smart again: ${smarts.debugginPrintProcessed()}")
            // TODO: Smart needs more work

            println("===============================================>")
            println("And a quick search, too:")

            val solution = imp.search()
            println("Solution: ${solution}:")
            for (individual in solution.individuals) {
                println(" --- ")
                println("${individual.individual.debugginPrint()} => ${individual.individual.debugginPrintProcessed()} => ${individual.fitness.computeFitnessScore()}")

            }

            val config = injector.getInstance(EMConfig::class.java)
            val rc = injector.getInstance(RemoteController::class.java)
            val controllerInfoDto = rc.getControllerInfo() ?:
            throw IllegalStateException(
                    "Cannot retrieve Remote Controller info from ${rc.host}:${rc.port}")


            /*
            TODO: Writing the tests is a bloody mess at the moment (for a variety of reasons, some self-inflicted). Fix that.
            TestSuiteWriter.writeTests(
                    solution,
                    controllerInfoDto.fullName,
                    config
            )
            */

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



    }

}