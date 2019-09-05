package org.evomaster.resource.rest.generator.implementation.java

/**
 * created by manzh on 2019-08-16
 */
interface SpringRestAPI {

    fun assertNonExistence(repository : String, idScript : String, exceptionStatusCode : Int= 400) =
            """
                if ($repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()

    fun assertExistence(repository : String, idScript : String, exceptionStatusCode : Int= 400) =
            """
                if (!$repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
            """.trimIndent()

    fun findEntityByIdAndAssigned(repository : String, idScript : String, target: String, targetType: String, exceptionStatusCode : Int= 400) =
            """
                if (! $repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
        
                $targetType $target =  $repository.findById($idScript).get();
            """.trimIndent()

    fun findOrCreateEntityByIdAndAssigned(repository : String, idScript : String, target: String, targetType: String, newInstanceMethod: String) =
            """
                $targetType $target = null;
                if ($repository.findById($idScript).isPresent())
                    $target =  $repository.findById($idScript).get();
                else
                    $target =  $newInstanceMethod;
        
                
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

    fun findEntityById(repository : String, idScript : String, entityType: String, target: String, exceptionStatusCode : Int= 400) =
            """
                if (! $repository.findById($idScript).isPresent())
                    return ResponseEntity.status($exceptionStatusCode).build();
                $entityType $target =  $repository.findById($idScript).get();
            """.trimIndent()

    fun repositorySave(repository: String, entityVar : String): String = "$repository.save($entityVar);"

    fun repositoryDeleteById(repository: String, entityVar : String): String = "$repository.deleteById($entityVar);"


    fun returnWithContent(variable : String) = "return ResponseEntity.ok($variable);"

    fun returnStatus(code : Int = 201) = "return ResponseEntity.status($code).build();"
}