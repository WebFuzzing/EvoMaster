package org.evomaster.resource.rest.generator.implementation.java

/**
 * created by manzh on 2019-08-16
 */
interface SpringRestAPI {

    fun assertNonExistence(repository : String, idScript : String, exceptionStatusCode : Int= 400) =
            """
                //an entity with id $idScript should not exist
                if ($repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()

    fun assertCondition(condition : String, not : Boolean = false , exceptionStatusCode : Int= 400) =
            """
                if (${if (not) "!" else ""}($condition))
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()

    fun assertExistence(repository : String, idScript : String, exceptionStatusCode : Int= 400) =
            """
                //an entity with id $idScript should exist
                if (!$repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()

    fun findEntityByIdAndAssigned(repository : String, idScript : String, target: String, targetType: String, checkNull : Boolean = false, exceptionStatusCode : Int= 400) =
            """
                ${if (checkNull) "if($idScript != null){" else ""}
                if (! $repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
                $targetType $target =  $repository.findById($idScript).get();
                ${if (checkNull) "}" else ""}
            """.trimIndent()

    fun findEntityByIdAndAssignedAndSave(repository : String, idScript : String, target: String, targetType: String,  settingScript: String, gettingScript : String, exceptionStatusCode : Int= 400) =
            """
                $targetType $target = null;
                if($idScript != null){
                    if (! $repository.findById($idScript).isPresent())
                        return ResponseEntity.status($exceptionStatusCode).build();
                    $target =  $repository.findById($idScript).get();
                    $settingScript
                }else{
                    $target = $gettingScript;
                }
            """.trimIndent()

    fun anyPropertiesNotNullUpdateEntity(repository: String, idScript: String, targetEntity : String,entityType: String, properties : List<String>, entitySetterProperties: List<String>) =
            """
                ${findEntityByIdAndAssigned(repository = repository, idScript = idScript, target = targetEntity, targetType = entityType)}
                if(${properties.joinToString("||") { "$it != null" }}){
                    ${properties.mapIndexed { index, p->  
                            "if($p != null) $targetEntity.${entitySetterProperties[index]}($p);" 
                        }.joinToString("")
                    }
                    ${repositorySave(repository=repository, entityVar = targetEntity)}
                }
            """.trimIndent()

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

    fun findAllEntitiesAndConvertToDto(repository : String, entityType: String, target: String, toDtoMethod: String, DtoType: String) =
            """
                List<$DtoType> $target = new ArrayList<>();
                for ($entityType e : $repository.findAll()){
                    $target.add(e.$toDtoMethod);
                }
            """.trimIndent()


    fun findEntityByIdAndConvertToDto(repository : String, idScript : String, entityType: String, target: String, targetType: String, toDtoMethod: String, exceptionStatusCode : Int= 400) =
            """
                if (! $repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
                $targetType $target =  $repository.findById($idScript).get().$toDtoMethod;
            """.trimIndent()

    fun entityConvertToDto(entityInstance: String, target: String, targetType: String, toDtoMethod: String, exceptionStatusCode : Int= 400) =
            """
                $targetType $target =  $entityInstance.$toDtoMethod;
            """.trimIndent()

    fun findEntityById(repository : String, idScript : String, entityType: String, target: String, exceptionStatusCode : Int= 400) =
            """
                if (! $repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
                $entityType $target =  $repository.findById($idScript).get();
            """.trimIndent()

    fun repositorySave(repository: String, entityVar : String): String = "$repository.save($entityVar);"

    fun repositoryDeleteById(repository: String, entityVar : String): String = "$repository.deleteById($entityVar);"

    fun returnWithContent(variable : String) = "return ResponseEntity.ok($variable);"

    fun returnStatus(code : Int = 201, msg : String? = null) = "return ResponseEntity.status($code)${if (msg == null) ".build()" else ".body($msg)"};"

}