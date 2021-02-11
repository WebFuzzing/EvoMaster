package org.evomaster.core.problem.graphql.service

import io.restassured.assertion.CookieMatcher.getCookies
import org.evomaster.client.java.controller.problem.GraphQlProblem
import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.IntrospectiveQuery
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.junit.Assert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie

internal class GraphQLFitnessTest{

    @Disabled
    fun testInvocation() {
/*
        val driver = NeedOne()
        val sutServer = driver.startSut()
        val info = driver.problemInfo as GraphQlProblem

        val urlGraphql = sutServer + info.endpoint

        val iq = IntrospectiveQuery()
        val schema = iq.fetchSchema(urlGraphql)

        val actionCluster = mutableMapOf<String, Action>()

        GraphQLActionBuilder.addActionsFromSchema(schema, actionCluster)

        val action = actionCluster.get("") as GraphQLAction
        val action2 = actionCluster.get("") as GraphQLAction

        val gQlF = GraphQLFitness()
        val gqlcr = GraphQlCallResult()

        val actions= mutableListOf<Action>()
        val dbAc = mutableListOf<DbAction>()

        */
    }


}


