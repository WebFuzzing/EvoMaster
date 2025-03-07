package org.evomaster.core.problem.rest.schema

import org.evomaster.core.remote.SutProblemException

/**
 * Given a schema file, the whole schema might not be included in just it.
 * A schema could have references to other schemas.
 * All those would need to be automatically retrieved and parsed.
 */
class RestSchema(
    val main: SchemaOpenAPI
) {



    init{
        //need to check for all $ref, recursively
        //TODO



    }

    fun validate(){
        if (main.schemaParsed.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved OpenAPI file")
        }
        // give the error message if there is nothing to test
        if (main.schemaParsed.paths.size == 0){
            throw SutProblemException("The OpenAPI file from ${main.sourceLocation} " +
                    "is either invalid or it does not define endpoints")
        }
    }
}