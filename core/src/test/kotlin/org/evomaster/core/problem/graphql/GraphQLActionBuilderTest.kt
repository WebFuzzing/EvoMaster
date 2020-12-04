package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Disabled

@Disabled
class GraphQLActionBuilderTest{


     @Test
     fun testPetClinic(){

         val actionCluster = mutableMapOf<String,Action>()
         val json = PetClinicCheckMain::class.java.getResource("/graphql/QueryTypeGlobalPetsClinic.json").readText()

         GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)

         assertEquals(15, actionCluster.size)

         val pettypes = actionCluster.get("pettypes")  as GraphQLAction
         assertEquals(1, pettypes.parameters.size)
         assertTrue(pettypes.parameters[0] is GQReturnParam)
         assertTrue(pettypes.parameters[0].gene is ArrayGene<*>)
         val objPetType = (pettypes.parameters[0].gene as ArrayGene<*>).template as ObjectGene
         assertEquals(2, objPetType.fields.size)
         assertTrue(objPetType.fields.any{ it is IntegerGene && it.name == "id"})
         assertTrue(objPetType.fields.any{ it is StringGene && it.name == "name"})
        /**/
         val pet = actionCluster.get("pet")  as GraphQLAction
         assertEquals(1, pet.parameters.size)
         assertTrue(pet.parameters[0] is GQReturnParam)
         assertTrue(pet.parameters[0].gene is ObjectGene)

         val specialties = actionCluster.get("specialties")  as GraphQLAction
         assertEquals(1, specialties.parameters.size)
         assertTrue(specialties.parameters[0] is GQReturnParam)
         assertTrue(specialties.parameters[0].gene is ArrayGene<*> )

         //TODO other assertions on the actions
     }
 }