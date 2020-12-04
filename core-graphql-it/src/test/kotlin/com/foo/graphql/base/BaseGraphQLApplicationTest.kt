package com.foo.graphql.base

import org.evomaster.client.java.controller.problem.GraphQlProblem
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.IntrospectiveQuery
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BaseGraphQLApplicationTest{


    @Disabled
    @Test
    fun testIntrospectiveQuery(){

        val driver = BaseGQDriver()
        //driver.startTheControllerServer()

        try {
            val sutServer = driver.startSut()
            val info = driver.problemInfo as GraphQlProblem

            val urlGraphql = sutServer + info.endpoint

            val iq = IntrospectiveQuery()
            val schema = iq.fetchSchema(urlGraphql)

            val actionCluster = mutableMapOf<String, Action>()

            GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster)

            assertEquals(1, actionCluster.size)
            val all = actionCluster.get("all")  as GraphQLAction
            assertEquals(1, all.parameters.size)
            //

           val arrayUser= (all.parameters[0].gene as OptionalGene).gene as ArrayGene<*>
           val objUser=arrayUser.template as ObjectGene
           assertEquals(4, objUser.fields.size)
           assertFalse(objUser.fields.get(0) is OptionalGene)
           assertTrue(objUser.fields.get(0) is StringGene)
           assertTrue((objUser.fields.get(3) as OptionalGene).gene is IntegerGene)
           assertTrue(objUser.fields.any{ it is StringGene && it.name == "id"})



        }catch(e: Exception){
            driver.stopSut()
            throw e
        }
    }
}