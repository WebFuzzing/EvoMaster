package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.implementation.java.AppliedJavaType
import org.evomaster.resource.rest.generator.implementation.java.app.JavaApp
import org.evomaster.resource.rest.generator.implementation.java.controller.em.JavaEMController
import org.evomaster.resource.rest.generator.implementation.java.controller.ex.JavaEXController
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind
import org.evomaster.resource.rest.generator.implementation.java.dto.JavaDto
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaEntity
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaEntityRepository
import org.evomaster.resource.rest.generator.implementation.java.service.JavaResourceAPI
import org.evomaster.resource.rest.generator.implementation.java.utils.RestDepUtil
import org.evomaster.resource.rest.generator.model.*
import org.evomaster.resource.rest.generator.pom.CSPOModel
import org.evomaster.resource.rest.generator.pom.EMPOModel
import org.evomaster.resource.rest.generator.pom.EXPOModel
import org.evomaster.resource.rest.generator.pom.PackagedPOModel
import org.evomaster.resource.rest.generator.template.ClassTemplate
import org.evomaster.resource.rest.generator.template.RegisterType
import kotlin.random.Random

/**
 * created by manzh on 2019-08-16
 */
class GenerateREST(val config: GenConfig, private var resourceGraph : ResourceGraph? = null) {

    companion object{
        const val DEFAULT_PROPERTY_NAME = "name"
        const val DEFAULT_PROPERTY_VALUE = "value"
    }

    private val resourceCluster = mutableMapOf<String, ResGenSpecification>()

    //only for debugging
    fun getResourceCluster() : Map<String, ResGenSpecification>{
        init()
        return resourceCluster.toMap()
    }

    fun run(){
        init()
        generatePOM()
        val type = registerType()
        when(config.outputContent){
            GenConfig.OutputContent.CS ->{
                generateAndSaveCS(type)
            }
            GenConfig.OutputContent.CS_EM ->{
                val cs = generateAndSaveCS(type)
                generateAndSaveEM(cs.name, type)
            }
            GenConfig.OutputContent.CS_EX ->{
                val cs = generateAndSaveCS(type)
                generateAndSaveEX(cs.name, type)
            }
            GenConfig.OutputContent.CS_EM_EX ->{
                val cs = generateAndSaveCS(type)
                generateAndSaveEM(cs.name, type)
                generateAndSaveEX(cs.name, type)
            }
        }
    }

    private fun init(){
        val graph = resourceGraph?:generateGraph()

        graph.save(config.getCsResourceFolder(), config.saveGraph)
        createResources(graph)
    }

    private fun generateGraph() : ResourceGraph{
        return resourceGraph?: ResourceGraph(
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
    }

    private fun generateAndSaveCS(type: RegisterType) : AppClazz{
        val cs = AppClazz(
                rootPackage = config.csProjectPackage,
                outputFolder = config.getCsOutputFolder(),
                resourceFolder = config.getCsResourceFolder()
        )
        generateAndSaveCS(JavaApp(cs), type)
        resourceCluster.values.forEach { generateAndSaveCS(it, type) }
        if (config.dependencyKind != ConditionalDependencyKind.EXISTENCE){
            val utilClazz = ServiceUtilClazz(
                    rootPackage = config.csProjectPackage,
                    outputFolder = config.getCsOutputFolder(),
                    resourceFolder = config.getCsResourceFolder()
            )
            generateAndSaveCS(RestDepUtil(utilClazz), type)
        }
        return cs
    }

    private fun generateAndSaveEM(appClazz: String, type: RegisterType){
        generateAndSaveCS(JavaEMController(AppClazz(
                name = config.emMainClass,
                rootPackage = config.emProjectPackage,
                outputFolder = config.getEmOutputFolder(),
                resourceFolder = config.getEmResourceFolder()),
                sutPackagePrefix = "${config.csProjectPackage}.",
                appClazz = appClazz
        ), type)
    }

    private fun generateAndSaveEX(appClazz: String, type: RegisterType){
        generateAndSaveCS(
                JavaEXController(
                        AppClazz(
                            name = config.exMainClass,
                            rootPackage = config.exProjectPackage,
                            outputFolder = config.getExOutputFolder(), resourceFolder = config.getExResourceFolder()),
                        csName = config.csName,
                        jarName = config.getCSJarName(),
                        sutPackagePrefix = "${config.csProjectPackage}.",
                        appClazz = appClazz,
                        rootProject = config.getCSRootProjectPathForExternal()
        ), type)
    }

    private fun generatePOM(){
        when(config.outputType){
            GenConfig.OutputType.MAVEN_PROJECT ->{
                val parent = PackagedPOModel(modules = getModules(), groupId = config.groupId, artifactId = config.projectName, output = config.getProjectFolder())
                parent.save()
                val cs = CSPOModel(groupId = config.groupId, artifactId = config.csName, output = config.getCsRootFolder(), repackageName = config.repackageName(),parent = parent)
                cs.save()

                when(config.outputContent){
                    GenConfig.OutputContent.CS_EM ->{
                        EMPOModel(groupId = config.groupId, artifactId = config.emName, output = config.getEmRootFolder(), csPOModel = cs, parent = parent).save()
                    }
                    GenConfig.OutputContent.CS_EX ->{
                        EXPOModel(groupId = config.groupId, artifactId = config.exName, output = config.getExRootFolder(), repackageName = config.getEXJarFinalName(),exClazz = config.getFullExMainClass(), csPOModel = cs, parent = parent).save()
                    }
                    GenConfig.OutputContent.CS_EM_EX ->{
                        EMPOModel(groupId = config.groupId, artifactId = config.emName, output = config.getEmRootFolder(), csPOModel = cs, parent = parent).save()
                        EXPOModel(groupId = config.groupId, artifactId = config.exName, output = config.getExRootFolder(), repackageName = config.getEXJarFinalName(), exClazz = config.getFullExMainClass(), csPOModel = cs, parent = parent).save()
                    }
                    else ->{
                        //do nothing
                    }
                }
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
                   resourceFolder = config.getCsResourceFolder(),
                   restMethods = getMethods(node),
                   createMethod = getCreation(),
                   dependencyKind = config.dependencyKind,
                   idProperty = PropertySpecification(config.idName, config.idType.name, isId = true, autoGen = false, allowNull = false, impactful = true),
                   defaultProperties = if (config.numOfExtraProperties == -1) mutableListOf(
                           PropertySpecification(DEFAULT_PROPERTY_NAME, CommonTypes.STRING.name, isId = false, autoGen = false, allowNull = false, impactful = true),
                           PropertySpecification(DEFAULT_PROPERTY_VALUE,
                                   CommonTypes.OBJ_INT.name, isId = false, autoGen = false, allowNull = false, impactful = true, dependency = config.dependencyKind, forAdditionalDependency = true)
                   )else generateProperties(config),
                   path = if(config.hideExistsDependency) "" else graph.getPath(node),
                   pathWithId = graph.getPathWithIds(node, config.idName, !config.hideExistsDependency),
                   pathParams = if (config.hideExistsDependency) listOf() else graph.getPathParams(node, config.idName)
           ))
        }

        resourceCluster.forEach { (t, u) ->
            u.initDependence(resourceCluster)
        }
    }

    private fun getMethods(node: ResNode) : List<RestMethod>{
        if(config.hideExistsDependency || node.outgoing.isEmpty())
            return config.restMethods.filter { it != RestMethod.DELETE_CON && it != RestMethod.GET_ALL_CON }
        return config.restMethods//.filter { it != RestMethod.DELETE && it != RestMethod.GET_ALL }
    }

    private fun getCreation() : RestMethod{
        val post = config.restMethods.filter { it == RestMethod.POST_ID || it == RestMethod.POST || it == RestMethod.POST_VALUE }
        if (post.size == 1) return post.first()
        throw IllegalArgumentException("incorrect rest methods setting")
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
                    dependency = config.dependencyKind,
                    forAdditionalDependency = true
            )
            pros.set(index, np)
        }
        return pros
    }

    private fun getModules() = when(config.outputContent){
        GenConfig.OutputContent.CS -> listOf(config.csName)
//        GenConfig.OutputContent.EM -> listOf(config.emName)
//        GenConfig.OutputContent.EX -> listOf(config.exName)
        GenConfig.OutputContent.CS_EM -> listOf(config.csName, config.emName)
        GenConfig.OutputContent.CS_EX ->listOf(config.csName, config.exName)
        GenConfig.OutputContent.CS_EM_EX ->listOf(config.csName, config.emName, config.exName)

    }

}




