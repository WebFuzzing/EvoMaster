package org.evomaster.core.problem.graphql

import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.evomaster.core.search.Action
import org.junit.jupiter.api.Disabled

@Disabled
class GraphQLActionBuilderTest{


     @Test
     fun testPetClinic(){

         val actionCluster = mutableMapOf<String,Action>()
         val json = PetClinicCheckMain::class.java.getResource("/graphql/QueryTypeGlobalPetsClinic.json").readText()

         GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)

         assertEquals(15, actionCluster.size)
         //TODO other assertions on the actions
     }
 }