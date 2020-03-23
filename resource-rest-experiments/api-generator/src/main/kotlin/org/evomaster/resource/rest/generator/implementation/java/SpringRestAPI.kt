package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet
import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippetType
import org.evomaster.resource.rest.generator.implementation.java.service.Utils
import org.evomaster.resource.rest.generator.model.ResServiceTypedPropertySpecification
import org.evomaster.resource.rest.generator.model.RestMethod

/**
 * created by manzh on 2019-08-16
 */
interface SpringRestAPI {

    fun assertNonExistence(repository : String, idScript : String, ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : Int= 400): String {
        val ifText = "if ($repository.findById($idScript).isPresent())"
        ifsnippets.add(IfSnippet(ifText, type = iftype))
        return """
                //an entity with id $idScript should not exist
                $ifText
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()
    }


    fun assertCondition(condition : String, not : Boolean = false , ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : Int= 400) :String{
        val ifText = "if (${if (not) "!" else ""}($condition))"
        ifsnippets.add(IfSnippet(ifText, type = iftype))
        return """
                    $ifText
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()
    }

    fun assertExistence(repository : String, idScript : String, ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : Int= 404) :String{
        val ifText = "if (!$repository.findById($idScript).isPresent())"
        ifsnippets.add(IfSnippet(ifText, type = iftype))
        return """
                //an entity with id $idScript should exist
                if (!$repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()
    }


    fun findEntityByIdAndAssigned(repository : String, idScript : String, target: String, targetType: String,
                                  ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : String = "404") :String{
        val ifText = "if (! $repository.findById($idScript).isPresent())"
        ifsnippets.add(IfSnippet(ifText, iftype))

        return """
                $ifText
                    return ResponseEntity.status($exceptionStatusCode).build();
                $targetType $target =  $repository.findById($idScript).get();
            """.trimIndent()
    }

    fun findEntityByIdAndAssignedAndSave(repository : String, idScript : String, target: String, targetType: String, settingScript: String, gettingScript : String,
                                         ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : Int= 400) : String {

        val ifCheckOptionalParam = "if($idScript != null)"
        ifsnippets.add(IfSnippet(ifCheckOptionalParam, IfSnippetType.CHECK_OPTIONAL_PARAM_EXISTENCE))
        val ifText = "if (! $repository.findById($idScript).isPresent())"
        ifsnippets.add(IfSnippet(ifText, iftype))
        return """
                $targetType $target = null;
                $ifCheckOptionalParam{
                    $ifText
                        return ResponseEntity.status($exceptionStatusCode).build();
                    $target =  $repository.findById($idScript).get();
                    $settingScript
                }else{
                    $target = $gettingScript;
                }
            """.trimIndent()
    }


    fun anyPropertiesNotNullUpdateEntity(repository: String, idScript: String, targetEntity : String,entityType: String,
                                         properties : List<String>, entitySetterProperties: List<String>,
                                         ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType):String {
        val overall = "if(${properties.joinToString("||") { "$it != null" }})"
        val optionalParam = mutableListOf(overall)
        val content= """
                ${findEntityByIdAndAssigned(repository = repository, idScript = idScript, target = targetEntity, targetType = entityType, ifsnippets = ifsnippets, iftype = iftype)}
                $overall{
                    ${properties.mapIndexed { index, p->
                        val condition = "if($p != null)"
                        optionalParam.add(condition)
                        "$condition $targetEntity.${entitySetterProperties[index]}($p);"
                    }.joinToString("")
                    }
                    ${repositorySave(repository=repository, entityVar = targetEntity)}
                }
        """.trimIndent()

        optionalParam.forEach {
            ifsnippets.add(IfSnippet(it, IfSnippetType.CHECK_OPTIONAL_PARAM_EXISTENCE))
        }
        return content
    }

    fun setIdOnPathToDto(idScript: String, idOnPath: String) = "$idScript = $idOnPath;"

    fun findOrCreateEntityByIdAndAssigned(repository : String, idScript : String, target: String, targetType: String, newInstanceMethod: String, idSetter : String) =
            """
                $targetType $target = null;
                if ($repository.findById($idScript).isPresent())
                    $target =  $repository.findById($idScript).get();
                else{
                    $target =  $newInstanceMethod;
                    $idSetter
                }
            """.trimIndent()

    fun findAllEntitiesAndConvertToDto(repository : String, entityType: String, target: String, toDtoMethod: String, DtoType: String) : String{
            return """
                List<$DtoType> $target = new ArrayList<>();
                for ($entityType e : $repository.findAll()){
                    $target.add(e.$toDtoMethod);
                }
            """.trimIndent()
    }



    fun findEntityByIdAndConvertToDto(repository : String, idScript : String, entityType: String, target: String, targetType: String,
                                      toDtoMethod: String, ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : Int= 404) : String{
        val ifText = "if (! $repository.findById($idScript).isPresent())"
        ifsnippets.add(IfSnippet(ifText, iftype))
        return """
                $ifText        
                    return ResponseEntity.status($exceptionStatusCode).build();
                $targetType $target =  $repository.findById($idScript).get().$toDtoMethod;
            """.trimIndent()
    }


    fun entityConvertToDto(entityInstance: String, target: String, targetType: String, toDtoMethod: String, exceptionStatusCode : Int= 404) =
            """
                $targetType $target =  $entityInstance.$toDtoMethod;
            """.trimIndent()

    fun findEntityById(repository : String, idScript : String, entityType: String, target: String, ifsnippets : MutableList<IfSnippet>, iftype: IfSnippetType, exceptionStatusCode : Int= 400) : String{
        val ifText = "if (! $repository.findById($idScript).isPresent())"
        ifsnippets.add(IfSnippet(ifText, iftype))
        return  """
                $ifText
                    return ResponseEntity.status($exceptionStatusCode).build();
                $entityType $target =  $repository.findById($idScript).get();
            """.trimIndent()
    }


    fun repositorySave(repository: String, entityVar : String): String = "$repository.save($entityVar);"

    fun repositoryDeleteById(repository: String, entityVar : String): String = "$repository.deleteById($entityVar);"

    fun returnWithContent(variable : String) = "return ResponseEntity.ok($variable);"

    fun returnStatus(code : Int = 201, msg : String? = null) = "return ResponseEntity.status($code)${if (msg == null) ".build()" else ".body($msg)"};"

    fun createOwnedEntityWithServiceApi(statusCode : String, apiVar: ResServiceTypedPropertySpecification, method: RestMethod, inputs : List<String>) = "int $statusCode = ${apiVar.name}.${Utils.generateRestMethodName(method, apiVar.resourceName)}(${inputs.joinToString(",")}).getStatusCode().value();"
}