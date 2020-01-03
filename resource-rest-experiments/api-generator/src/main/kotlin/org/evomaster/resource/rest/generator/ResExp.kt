package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind
import org.evomaster.resource.rest.generator.model.StrategyNameResource
import kotlin.math.roundToInt

/**
 * created by manzh on 2019-12-19
 */

object ResExp{

    fun n5_dense() : GenConfig{
        val config_medium_5 = defaultN5()
        config_medium_5.numOfTwoToTwo = 1

//        //branch
//        config_medium_5.numOfExtraProperties = 2
//        config_medium_5.branchesForImpact = 2

        config_medium_5.projectName ="n5-dense"
        return config_medium_5
    }

    fun n5_medium() : GenConfig{
        val config_medium_5 = defaultN5()
        config_medium_5.numOfOneToOne = 3

//        //branch
//        config_medium_5.numOfExtraProperties = 2
//        config_medium_5.branchesForImpact = 2

        //additional dependency
        config_medium_5.dependencyKind = ConditionalDependencyKind.PROPERTY

        config_medium_5.projectName ="n5-medium"
        return config_medium_5
    }

    private fun defaultN5() : GenConfig{
        val n5 = GenConfig()
        n5.numOfNodes = 5
        n5.outputType = GenConfig.OutputType.MAVEN_PROJECT
        n5.outputContent = GenConfig.OutputContent.CS_EM_EX
        n5.nameStrategy = StrategyNameResource.RAND_FIXED_LENGTH
        return n5
    }

    fun setDependency(numOfNode : Int, config: GenConfig, type: DependencyType){
        val numOfEdge = type.density * numOfNode * (numOfNode - 1)

        // 1:1 -> 50%, 2:1 -> 20%, 1:2->20%, 1:3 ->10%, 3:1->10%, 2:2 -> 10%
        config.numOfOneToOne = (numOfEdge * 0.5).toInt()
        config.numOfOneToTwo = (numOfEdge * 0.2 /2).roundToInt()
        config.numOfTwoToOne = (numOfEdge * 0.2 /2).roundToInt()
        config.numOfOneToMany = (numOfEdge * 0.2 /3).roundToInt()
        config.numOfManyToOne = (numOfEdge * 0.2 /3).roundToInt()
        config.numOfTwoToTwo = (numOfEdge * 0.1).roundToInt()

        val total = config.numOfOneToOne + config.numOfOneToTwo * 2 + config.numOfTwoToOne *2 + config.numOfOneToMany * 3 + config.numOfManyToOne * 3 + config.numOfTwoToTwo * 4
        if(total > numOfEdge){
            val removal = total - numOfEdge
        }
    }
    //density = e/(v(v-1)), 0.25, 0.5,0.75
    enum class DependencyType (val density : Double){
        NONE (0.0),
        SPARSE (0.25),
        MEDIUM(0.5),
        DENSE(0.75)
    }
}

fun main(args : Array<String>){

    GenerateREST(ResExp.n5_dense()).run()
}