package org.evomaster.core.problem.graphql


import com.google.gson.Gson
import org.evomaster.core.problem.graphql.builder.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.search.action.Action

class PetClinicCheckMain {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            //reading the gson file
            val json = PetClinicCheckMain::class.java.getResource("/graphql/online/PetsClinic.json").readText()

            //converting json to object
            val gson = Gson()
            val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
            println("From JSON To OBJECT:\n" + schemaObj)


            val actionCluster = mutableMapOf<String, Action>()

            GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
            for (element in actionCluster) {
                println(element)
            }


        }


    }
}