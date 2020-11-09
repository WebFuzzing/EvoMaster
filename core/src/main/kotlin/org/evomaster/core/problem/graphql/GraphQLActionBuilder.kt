package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.search.Action


object GraphQLActionBuilder {


    /**
     * @param schema: the schema extracted from a GraphQL API, as a JSON string
     * @param actionCluster: for each mutation/query in the schema, populate this map with
     *                      new action templates.
     */
    fun addActionsFromSchema(schema: String, actionCluster: MutableMap<String, Action>){

        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(schema, SchemaObj::class.java)


        /*
         TOODO

            - go through every Query and Mutation
            - create action for it which the needed genes
            - add the action to actionCluster
         */
    }

}