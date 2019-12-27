package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.implementation.java.AppliedJavaType
import org.evomaster.resource.rest.generator.implementation.java.app.JavaApp
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind
import org.evomaster.resource.rest.generator.implementation.java.dto.JavaDto
import org.evomaster.resource.rest.generator.implementation.java.em.JavaEMController
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaEntity
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaEntityRepository
import org.evomaster.resource.rest.generator.implementation.java.service.JavaResourceAPI
import org.evomaster.resource.rest.generator.implementation.java.utils.RestDepUtil
import org.evomaster.resource.rest.generator.model.*
import org.evomaster.resource.rest.generator.pom.CSPOModel
import org.evomaster.resource.rest.generator.pom.EMPOModel
import org.evomaster.resource.rest.generator.pom.PackagedPOModel
import org.evomaster.resource.rest.generator.template.ClassTemplate
import org.evomaster.resource.rest.generator.template.RegisterType
import kotlin.random.Random

/**
 * created by manzh on 2019-08-16
 */
class GenerateREST(val config: GenConfig) {

    companion object{
        const val DEFAULT_PROPERTY_NAME = "name"
        const val DEFAULT_PROPERTY_VALUE = "value"
    }

    private val resourceCluster = mutableMapOf<String, ResGenSpecification>()

    fun run(){
        init()
        generatePOM()
        val type = registerType()
        when(config.outputContent){
            GenConfig.OutputContent.CS ->{
                generateAndSaveCS(type)
            }
            GenConfig.OutputContent.EM ->{
                generateAndSaveEM("ResApp",type)
            }
            else ->{
                val cs = generateAndSaveCS(type)
                generateAndSaveEM(cs.name, type)
            }

        }

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
                ),
                strategyNameResource = config.nameStrategy
        )
        graph.save(config.getCsResourceFolder())
        createResources(graph)
    }

    private fun generateAndSaveCS(type: RegisterType) : AppClazz{
        val cs = AppClazz(
                rootPackage = config.csProjectPackage,
                outputFolder = config.getCsOutputFolder()
        )
        generateAndSaveCS(JavaApp(cs), type)
        resourceCluster.values.forEach { generateAndSaveCS(it, type) }
        if (config.dependencyKind != ConditionalDependencyKind.EXISTENCE){
            val utilClazz = ServiceUtilClazz(
                    rootPackage = config.csProjectPackage,
                    outputFolder = config.getCsOutputFolder()
            )
            generateAndSaveCS(RestDepUtil(utilClazz), type)
        }
        return cs
    }

    private fun generateAndSaveEM(appClazz: String, type: RegisterType){
        generateAndSaveCS(JavaEMController(AppClazz(
                name = "EmbeddedEvoMasterController",
                rootPackage = config.emProjectPackage,
                outputFolder = config.getEmOutputFolder()),
                sutPackagePrefix = "${config.csProjectPackage}.",
                appClazz = appClazz
        ), type)
    }


    private fun generatePOM(){
        when(config.outputType){
            GenConfig.OutputType.MAVEN_PROJECT ->{
                val parent = PackagedPOModel(modules = mutableListOf(config.csName, config.emName),groupId = config.groupId, artifactId = config.projectName, output = config.getProjectFolder())
                parent.save()
                val cs = CSPOModel(groupId = config.groupId, artifactId = config.csName, output = config.getCsRootFolder(), parent = parent)
                cs.save()
                EMPOModel(groupId = config.groupId, artifactId = config.emName, output = config.getEmRootFolder(), csPOModel = cs, parent = parent).save()
            }
            GenConfig.OutputType.MAVEN_MODULE ->{
                CSPOModel(config.groupId, config.csName, output = config.getCsRootFolder()).save()
            }
            else ->{
                //do nothing
            }
        }
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
                   restMethods = config.restMethods,
                   dependencyKind = config.dependencyKind,
                   defaultProperties = if (config.numOfExtraProperties == -1) mutableListOf(
                           PropertySpecification(DEFAULT_PROPERTY_NAME, CommonTypes.STRING.name, isId = false, autoGen = false, allowNull = false, impactful = true),
                           PropertySpecification(DEFAULT_PROPERTY_VALUE, CommonTypes.INT.name, isId = false, autoGen = false, allowNull = false, impactful = true, dependency = config.dependencyKind, forDependency = true)
                   )else generateProperties(config)
           ))
        }

        resourceCluster.forEach { (t, u) ->
            u.initDependence(resourceCluster)
        }
    }

    private fun generateProperties(config: GenConfig) : List<PropertySpecification>{
        val types = config.propertiesTypes
        val pros = mutableListOf<PropertySpecification>()
        if (config.numOfExtraProperties > 0){
            var counterImpact = 0
            (0 until config.numOfExtraProperties).forEach { i->
                val impactful = counterImpact < config.numOfImpactProperties
                pros.add(PropertySpecification("${if(impactful) "im" else "no"}Prop$i", types[Random.nextInt(0, types.size)].name, isId = false, autoGen = false, allowNull = !impactful, impactful = impactful, branches = if (impactful)config.branchesForImpact else 0))
                counterImpact+=1
            }
        }
        //FIXME
        if (config.dependencyKind != ConditionalDependencyKind.EXISTENCE){
            val p = pros.filter { it.impactful }.shuffled().first()
            val index = pros.indexOf(p)
            val np = PropertySpecification(
                    name = p.name,
                    type = p.type,
                    isId = p.isId,
                    autoGen = p.autoGen,
                    allowNull = p.allowNull,
                    impactful = p.impactful,
                    branches = p.branches,
                    dependency = config.dependencyKind
            )
            pros.set(index, np)
        }
        return pros
    }

}

fun main(args : Array<String>){
    val config = GenConfig()

//    config.outputFolder = "e2e-tests/spring-examples/"
//    config.outputType = GenConfig.OutputType.SOURCE
//    config.csProjectPackage = "com.foo.rest.examples.spring.impacts"
    config.numOfNodes = 50
    config.numOfImpactProperties = 2
    config.numOfExtraProperties = 8
//    config.numOfOneToOne = 1
    config.restMethods = listOf(RestMethod.POST_VALUE, RestMethod.GET_ALL, RestMethod.GET_ID)
    config.branchesForImpact = 10
    config.outputType = GenConfig.OutputType.MAVEN_PROJECT
    config.outputContent = GenConfig.OutputContent.BOTH
    config.dependencyKind = ConditionalDependencyKind.PROPERTY
    config.projectName = "auto-dependency-rest-cs-RM${config.restMethods.size}-N${config.numOfNodes}-P${config.numOfExtraProperties}-IMP${config.numOfImpactProperties}-B${config.branchesForImpact}"
    GenerateREST(config).run()
}




