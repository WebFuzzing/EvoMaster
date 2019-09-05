package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.model.ResourceGraph
import org.evomaster.resource.rest.generator.model.RestMethod
import org.evomaster.resource.rest.generator.implementation.java.AppliedJavaType
import org.evomaster.resource.rest.generator.implementation.java.app.JavaApp
import org.evomaster.resource.rest.generator.implementation.java.dto.JavaDto
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaEntity
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaEntityRepository
import org.evomaster.resource.rest.generator.implementation.java.service.JavaResourceAPI
import org.evomaster.resource.rest.generator.model.AppClazz
import org.evomaster.resource.rest.generator.model.EdgeMultiplicitySpecification
import org.evomaster.resource.rest.generator.model.ResGenSpecification
import org.evomaster.resource.rest.generator.pom.CSPOModel
import org.evomaster.resource.rest.generator.template.ClassTemplate
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-08-16
 */
class GenerateREST(val config: GenConfig) {

    private val resourceCluster = mutableMapOf<String, ResGenSpecification>()

    fun run(){
        init()
        generateAndSaveCS()
    }

    private fun init(){
        val graph = ResourceGraph(
                numOfNodes = config.numOfNodes,
                multiplicity = listOf(
                        EdgeMultiplicitySpecification(config.numOfOneToOne, 1,1),
                        EdgeMultiplicitySpecification(config.numOfOneToTwo, 1,2),
                        EdgeMultiplicitySpecification(config.numOfOneToMany, 1,3),
                        EdgeMultiplicitySpecification(config.numOfTwoToOne, 2,1),
                        EdgeMultiplicitySpecification(config.numOfManyToOne, 3,1),
                        EdgeMultiplicitySpecification(config.numOfTwoToTwo, 2,2),
                        EdgeMultiplicitySpecification(config.numOfManyToMany, 3,3)
                )
        )
        graph.save(config.getCsResourceFolder())
        createResources(graph)
    }

    private fun generateAndSaveCS(){
        val type = registerType()
        generateAndSaveCS(JavaApp(AppClazz(
                rootPackage = config.csProjectPackage,
                outputFolder = config.getCsOutputFolder()
            )
        ), type)
        resourceCluster.values.forEach { generateAndSaveCS(it, type) }

        if (config.outputType != GenConfig.OutputType.SOURCE)
            CSPOModel(config.csName, config.csName, output = config.getCsRootFolder()).save()
    }

    private fun registerType() : RegisterType {
        val dtos = resourceCluster.values.map { it.nameDtoClass() }.toList()

        val common =  when(config.language){
            GenConfig.Format.JAVA_SPRING_SWAGGER -> AppliedJavaType()
        }

        val generic = common.getGenericTypes(dtos)
        return RegisterType(common, resourceCluster.values.flatMap { it.produceTypes() }.toSet().plus(generic.values))
    }


    private fun generateAndSaveCS(gen : ResGenSpecification, type: RegisterType){
        when(config.language){
            GenConfig.Format.JAVA_SPRING_SWAGGER -> {
                generateAndSaveCS(JavaDto(gen.getDto()), type)
                generateAndSaveCS(JavaEntity(gen.getEntity()), type)
                gen.getRepository()?.apply { generateAndSaveCS(JavaEntityRepository(this), type) }
                gen.getApiService()?.apply { generateAndSaveCS(JavaResourceAPI(this), type) }
            }
        }
    }

    private fun <T> generateAndSaveCS(imp : T, type : RegisterType) where T: ClassTemplate{
        imp.generateAndSave(type)
    }

    private fun createResources(graph : ResourceGraph){
        graph.nodes.values.forEach {node->
           resourceCluster.putIfAbsent(node.name, ResGenSpecification(
                   resNode = node,
                   rootPackage = config.csProjectPackage,
                   outputFolder = config.getCsOutputFolder(),
                   restMethods = config.restMethods
           ))
        }

        resourceCluster.forEach { (t, u) ->
            u.initDependence(resourceCluster)
        }
    }
}

fun main(args : Array<String>){
    val config = GenConfig()

    config.outputFolder = "e2e-tests/spring-examples/"
    config.outputType = GenConfig.OutputType.SOURCE
    config.csProjectPackage = "com.foo.rest.examples.spring.resource"
    config.numOfNodes = 3
    config.numOfOneToOne = 1
    config.restMethods = listOf(RestMethod.POST, RestMethod.GET_ID)

    GenerateREST(config).run()
}

