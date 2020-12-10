package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Disabled


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
         val vets = actionCluster.get("vets")  as GraphQLAction
         assertEquals(1, vets.parameters.size)
         assertTrue(vets.parameters[0] is GQReturnParam)
         assertTrue(vets.parameters[0].gene is ArrayGene<*> )
         val objVets = (vets.parameters[0].gene as ArrayGene<*>).template as ObjectGene
         assertEquals(4, objVets.fields.size)
         assertTrue(objVets.fields.any{ it is IntegerGene && it.name == "id"})
         assertTrue(objVets.fields.any{ it is StringGene && it.name == "firstName"})
         assertTrue(objVets.fields.any{ it is StringGene && it.name == "lastName"})
         assertTrue(objVets.fields.any{ it is ArrayGene<*> && it.name == "Specialty"})
         val objSpecialty= (objVets.fields[3] as ArrayGene<*>).template as ObjectGene
         assertEquals(2, objSpecialty.fields.size)
         assertTrue(objSpecialty.fields.any{ it is IntegerGene && it.name == "id"})
         assertTrue(objSpecialty.fields.any{ it is StringGene && it.name == "name"})
         /**/
         val owners = actionCluster.get("owners")  as GraphQLAction
         assertEquals(1, vets.parameters.size)
         assertTrue(owners.parameters[0] is GQReturnParam)
         assertTrue(owners.parameters[0].gene is ArrayGene<*> )
         val owner = (owners.parameters[0].gene as ArrayGene<*>).template as ObjectGene
         assertEquals(7, owner.fields.size)
         assertTrue(owner.fields.any{ it is IntegerGene && it.name == "id"})
         assertTrue(owner.fields.any{ it is StringGene && it.name == "firstName"})
         assertTrue(owner.fields.any{ it is StringGene && it.name == "lastName"})
         assertTrue(owner.fields.any{ it is StringGene && it.name == "address"})
         assertTrue(owner.fields.any{ it is StringGene && it.name == "city"})
         assertTrue(owner.fields.any{ it is StringGene && it.name == "telephone"})
         assertTrue(owner.fields.any{ it is ArrayGene<*> && it.name == "Pet"})
         val objPet= (owner.fields[6] as ArrayGene<*>).template as ObjectGene
         assertEquals(6, objPet.fields.size)
         assertTrue(objPet.fields.any{ it is IntegerGene && it.name == "id"})
         assertTrue(objPet.fields.any{ it is StringGene && it.name == "name"})
         assertTrue(objPet.fields.any{ it is DateGene && it.name == "birthDate"})
         assertTrue(objPet.fields.any{ it is ObjectGene && it.name == "PetType"})
         assertTrue(objPet.fields.any{ it is ObjectGene && it.name == "Owner"})
         assertTrue(objPet.fields.any{ it is ObjectGene && it.name == "VisitConnection"})
         val objVisitConnection= objPet.fields[5] as ObjectGene
        // assertEquals(2, objVisitConnection.fields.size) not yet
         //assertTrue(objVisitConnection.fields[0] is IntegerGene) not yet
         //assertTrue(objVisitConnection.fields.any{ it is IntegerGene && it.name == "totalCount"}) not yet
         /**/
         val pet = actionCluster.get("pet")  as GraphQLAction
         assertEquals(1, pet.parameters.size)
         assertTrue(pet.parameters[0] is GQReturnParam)
         assertTrue(pet.parameters[0].gene is ObjectGene)
        /**/
         val specialties = actionCluster.get("specialties")  as GraphQLAction
         assertEquals(1, specialties.parameters.size)
         assertTrue(specialties.parameters[0] is GQReturnParam)
         assertTrue(specialties.parameters[0].gene is ArrayGene<*> )
         //TODO other assertions on the actions
     }
 }