package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind
import org.evomaster.resource.rest.generator.model.StrategyNameResource
import org.evomaster.resource.rest.generator.pom.DependencyManager
import org.evomaster.resource.rest.generator.pom.PackagedPOModel
import kotlin.math.roundToInt

/**
 * created by manzh on 2019-12-19
 */

object ResExp{

    const val groupId = "evo.artificial.resource.exp"

    fun n5_dense(enablePropertyDependency : Boolean = false) : GenConfig{
        val dense = default(5, enablePropertyDependency)
        dense.numOfTwoToTwo = 1

        dense.projectName = "n5-dense${if (enablePropertyDependency)"-pd" else ""}"
        setCSEMEX(dense)
        return dense
    }

    fun n5_medium(enablePropertyDependency : Boolean = false) : GenConfig{
        val config_medium_5 = default(5, enablePropertyDependency)
        config_medium_5.numOfOneToOne = 3

        config_medium_5.projectName ="n5-medium${if (enablePropertyDependency)"-pd" else ""}"
        setCSEMEX(config_medium_5)
        return config_medium_5
    }

    fun n5_sparse(enablePropertyDependency : Boolean = false) : GenConfig{
        val sparse = default(5, enablePropertyDependency)
        sparse.numOfOneToOne = 1

        sparse.projectName ="n5-sparse${if (enablePropertyDependency)"-pd" else ""}"
        setCSEMEX(sparse)

        return sparse
    }

    private fun default(node: Int, enablePropertyDependency: Boolean) : GenConfig{
        val config = GenConfig()
        config.numOfNodes = node
        config.groupId = groupId
        config.outputType = GenConfig.OutputType.MAVEN_PROJECT
        config.outputContent = GenConfig.OutputContent.CS_EM_EX
        config.nameStrategy = StrategyNameResource.RAND_FIXED_LENGTH
        if (enablePropertyDependency)
            config.dependencyKind = ConditionalDependencyKind.PROPERTY
        return config
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

    private fun setCSEMEX(config: GenConfig){
        config.csName = "${config.projectName}-cs"
        config.emName = "${config.projectName}-em"
        config.exName = "${config.projectName}-ex"
    }
}

fun main(args : Array<String>){

    val exp = arrayOf(false, true).flatMap {
        listOf(ResExp.n5_dense(it),ResExp.n5_medium(it),ResExp.n5_sparse(it))
    }

    val folder  = "/Users/mazh001/Documents/GitHub/artificial_rest_experiment/n5"
    val expFolder = PackagedPOModel(modules = exp.map { it.projectName }, groupId = ResExp.groupId, artifactId = "n5", output = folder)
    expFolder.save()

    exp.forEach {
        it.outputFolder = folder
        GenerateREST(it).run()
    }
    Util.generateDeployScript(exp, DependencyManager.defined_version.getValue(DependencyManager.EVOMASTER_CLIENT_JAVA_INSTRUMENTATION.versionKey), folder)

}