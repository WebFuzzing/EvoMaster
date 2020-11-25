package com.foo.graphql.base

import org.evomaster.client.java.controller.problem.GraphQlProblem
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.IntrospectiveQuery
import org.evomaster.core.search.Action
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
            assertEquals(4, all.parameters.size)

        }catch(e: Exception){
            driver.stopSut()
            throw e
        }
    }
}