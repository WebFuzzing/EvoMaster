package com.foo.graphql.base

import org.evomaster.client.java.controller.problem.GraphQlProblem
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.builder.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.IntrospectiveQuery
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BaseGraphQLApplicationTest{



    @Test
    fun testIntrospectiveQuery(){

        val driver = BaseGQDriver()

        try {
            val sutServer = driver.startSut()
            val info = driver.problemInfo as GraphQlProblem

            val urlGraphql = sutServer + info.endpoint

            val iq = IntrospectiveQuery()
            val headers= listOf<String>()
                .filter { it.isNotBlank() }
            val schema = iq.fetchSchema(urlGraphql,headers)

            val actionCluster = mutableMapOf<String, Action>()

            if (schema != null) {
                GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster)
            }

            assertEquals(1, actionCluster.size)
            val all = actionCluster.get("all")  as GraphQLAction
            assertEquals(1, all.parameters.size)
            //
           val objUser=all.parameters[0].gene as ObjectGene
           assertEquals(4, objUser.fields.size)

           assertTrue(objUser.fields.first { it.name == "id" } is BooleanGene)
           assertTrue(objUser.fields.first { it.name == "name" } is BooleanGene)
           assertTrue(objUser.fields.first { it.name == "surname" } is BooleanGene)
           assertTrue(objUser.fields.any{ it is BooleanGene && it.name == "age"})
            
        }catch(e: Exception){
            driver.stopSut()
            throw e
        }
    }
}