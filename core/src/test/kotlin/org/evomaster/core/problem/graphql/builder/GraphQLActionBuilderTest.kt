package org.evomaster.core.problem.graphql.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.Integer.max


class GraphQLActionBuilderTest {


    @Test
    fun testPetClinic() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/PetsClinic.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(15, actionCluster.size)

        val pettypes = actionCluster["pettypes"] as GraphQLAction
        assertEquals(1, pettypes.parameters.size)
        assertTrue(pettypes.parameters[0] is GQReturnParam)
        assertTrue(pettypes.parameters[0].gene is ObjectGene)
        val objPetType = pettypes.parameters[0].gene as ObjectGene
        assertEquals(2, objPetType.fields.size)
        assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "name" })
        val gQlReturn = GQReturnParam(pettypes.parameters[0].name, pettypes.parameters[0].gene)
        val gQlInputcopy = gQlReturn.copy()
        assertEquals(gQlReturn.name, gQlInputcopy.name)
        assertEquals(gQlReturn.gene.name, gQlInputcopy.gene.name)
        /**/
        val vets = actionCluster["vets"] as GraphQLAction
        assertEquals(1, vets.parameters.size)
        assertTrue(vets.parameters[0] is GQReturnParam)
        assertTrue(vets.parameters[0].gene is ObjectGene)
        val objVets = vets.parameters[0].gene as ObjectGene
        assertEquals(4, objVets.fields.size)
        assertTrue(objVets.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objVets.fields.any { it is BooleanGene && it.name == "firstName" })
        assertTrue(objVets.fields.any { it is BooleanGene && it.name == "lastName" })
        assertTrue(objVets.fields.any { it is OptionalGene && it.name == "specialties" })

        val objSpecialty = (objVets.fields.first { it.name == "specialties" } as OptionalGene).gene as ObjectGene
        assertEquals(2, objSpecialty.fields.size)
        assertTrue(objSpecialty.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objSpecialty.fields.any { it is BooleanGene && it.name == "name" })
        /**/
        val owners = actionCluster["owners"] as GraphQLAction
        assertEquals(3, owners.parameters.size)
        assertTrue(owners.parameters[0] is GQInputParam)
        assertTrue(owners.parameters[0].name == "filter")
        assertTrue((owners.parameters[0].gene as OptionalGene).gene is ObjectGene)
        val objOwnerFilter = (owners.parameters[0].gene as OptionalGene).gene as ObjectGene
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "firstName" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "lastName" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "address" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "city" })
        assertTrue(objOwnerFilter.fields.any { it is OptionalGene && it.name == "telephone" })
        assertTrue(owners.parameters[1] is GQInputParam)
        assertTrue(owners.parameters[1].name == "orders")
        assertTrue(owners.parameters[2] is GQReturnParam)
        assertTrue(owners.parameters[2].gene is ObjectGene)
        /**/
        val owner = owners.parameters[2].gene as ObjectGene
        assertEquals(7, owner.fields.size)
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "firstName" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "lastName" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "address" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "city" })
        assertTrue(owner.fields.any { it is BooleanGene && it.name == "telephone" })
        assertTrue(owner.fields.any { it is OptionalGene && it.name == "pets" })
        val objPet = ((owner.fields.first { it.name == "pets" }) as OptionalGene).gene as ObjectGene
        assertEquals(6, objPet.fields.size)
        assertTrue(objPet.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPet.fields.any { it is BooleanGene && it.name == "name" })
        assertTrue(objPet.fields.any { it is BooleanGene && it.name == "birthDate" })
        assertTrue(objPet.fields.any { it is OptionalGene && it.name == "type" })
        assertTrue(objPet.fields.any { it is OptionalGene && it.name == "visits" })
        assertTrue(objPet.fields[5] is OptionalGene)
        val objVisitConnection = (objPet.fields[5] as OptionalGene).gene as ObjectGene
        assertEquals(2, objVisitConnection.fields.size)
        assertTrue(objVisitConnection.fields[0] is BooleanGene)
        assertTrue(objVisitConnection.fields.any { it is BooleanGene && it.name == "totalCount" })
        assertTrue(objVisitConnection.fields.any { it is OptionalGene && it.name == "visits" })
        GeneUtils.repairBooleanSelection(owner); // this should not fail
        /**/
        val pet = actionCluster["pet"] as GraphQLAction
        assertEquals(2, pet.parameters.size)
        assertTrue(pet.parameters[0] is GQInputParam)
        assertTrue(pet.parameters[0].gene is IntegerGene)
        assertTrue(pet.parameters[0].gene.name == "id")
        assertTrue(pet.parameters[1] is GQReturnParam)
        assertTrue(pet.parameters[1].gene is ObjectGene)
        val objPet2 = (pet.parameters[1].gene as ObjectGene)
        assertEquals(6, objPet2.fields.size)
        assertTrue(objPet2.fields.any { it is OptionalGene && it.name == "visits" })
        GeneUtils.repairBooleanSelection(objPet2); // this should not fail
        /**/
        val specialties = actionCluster["specialties"] as GraphQLAction
        assertEquals(1, specialties.parameters.size)
        assertTrue(specialties.parameters[0] is GQReturnParam)
        assertTrue(specialties.parameters[0].gene is ObjectGene)

    }


    @Test
    fun bitquerySchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Bitquery.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(12, actionCluster.size)

        val algorand = actionCluster["algorand"] as GraphQLAction
        assertEquals(2, algorand.parameters.size)
        assertTrue(algorand.parameters[0] is GQInputParam)
        assertTrue(algorand.parameters[1] is GQReturnParam)
        assertTrue(algorand.parameters[1].gene is ObjectGene)
        val objAlgorand = algorand.parameters[1].gene as ObjectGene
        assertEquals(7, objAlgorand.fields.size)
        assertTrue(objAlgorand.fields.any { it is TupleGene && it.name == "address" })

    }

    @Test
    fun catalysisHubSchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/CatalysisHub.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(11, actionCluster.size)

    }

    @Test
    fun contentfulSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Contentful.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(22, actionCluster.size)

        val asset = actionCluster["asset"] as GraphQLAction
        assertEquals(4, asset.parameters.size)
        assertTrue(asset.parameters[0] is GQInputParam)
        assertTrue(asset.parameters[1] is GQInputParam)
        assertTrue(asset.parameters[2] is GQInputParam)
        assertTrue(asset.parameters[3] is GQReturnParam)
        assertTrue(asset.parameters[0].gene is StringGene)
        assertTrue((asset.parameters[1].gene as OptionalGene).gene is BooleanGene)
        assertTrue(asset.parameters[3].gene is ObjectGene)
        /**/
        val categoryCollection = actionCluster["categoryCollection"] as GraphQLAction
        assertEquals(7, categoryCollection.parameters.size)
        assertTrue(categoryCollection.parameters[0] is GQInputParam)
        assertTrue(categoryCollection.parameters[1] is GQInputParam)
        assertTrue(categoryCollection.parameters[2] is GQInputParam)
        assertTrue(categoryCollection.parameters[6] is GQReturnParam)
        assertTrue((categoryCollection.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue((categoryCollection.parameters[4].gene as OptionalGene).gene is ObjectGene)
        assertTrue((((categoryCollection.parameters[4].gene as OptionalGene).gene as ObjectGene).fields[6] as OptionalGene).gene is StringGene)
    }

    @Test
    fun countriesSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Countries.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(6, actionCluster.size)

        val continents = actionCluster["continents"] as GraphQLAction
        assertEquals(2, continents.parameters.size)
        assertTrue(continents.parameters[0] is GQInputParam)
        assertTrue(continents.parameters[1] is GQReturnParam)
        assertTrue(continents.parameters[1].gene is ObjectGene)
        val objContinents = continents.parameters[1].gene as ObjectGene
        assertTrue(objContinents.fields[2] is OptionalGene)
        val objCountry = (objContinents.fields[2] as OptionalGene).gene as ObjectGene
        assertTrue(objCountry.fields[7] is OptionalGene)

    }

    @Test
    fun deutscheBahnSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/DeutscheBahn.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(7, actionCluster.size)

        val routing = actionCluster["routing"] as GraphQLAction
        assertEquals(3, routing.parameters.size)
        assertTrue(routing.parameters[0] is GQInputParam)
        assertTrue(routing.parameters[2] is GQReturnParam)
        assertTrue(routing.parameters[2].gene is ObjectGene)

    }

    @Test
    fun digitransitHSLSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/DigitransitHSL.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(33, actionCluster.size)

        val node = actionCluster["node"] as GraphQLAction
        assertEquals(2, node.parameters.size)
        assertTrue(node.parameters[1] is GQReturnParam)

        assertTrue(node.parameters[1].gene is ObjectGene)
        val interfaceObjectNode = node.parameters[1].gene as ObjectGene
        assertEquals(15, interfaceObjectNode.fields.size)

        assertTrue(interfaceObjectNode.fields[0] is OptionalGene)
        assertTrue((interfaceObjectNode.fields[0] as OptionalGene).gene is ObjectGene)
        val objAgency = (interfaceObjectNode.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(9, objAgency.fields.size)
        assertTrue(objAgency.fields.any { it is BooleanGene && it.name == "lang" })
        assertTrue(objAgency.fields.any { it is BooleanGene && it.name == "phone" })
    }

    @Test
    fun eHRISchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/EHRI.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(19, actionCluster.size)

    }

    @Test
    fun etMDBSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/EtMDB.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(24, actionCluster.size)

    }

    @Test
    fun everbaseSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Everbase.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(14, actionCluster.size)

    }


    @Test
    fun graphQLJobsSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/GraphQLJobs.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(15, actionCluster.size)

    }

    @Test
    fun HIVDBSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/HIVDB.json").readText()
        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(9, actionCluster.size)

    }

    @Test
    fun melodyRepoSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/MelodyRepo.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(2, actionCluster.size)

        val ppackage = actionCluster["package"] as GraphQLAction
        assertEquals(2, ppackage.parameters.size)
        assertTrue(ppackage.parameters[0] is GQInputParam)
        assertTrue(ppackage.parameters[0].gene is StringGene)
        val objPackage = ppackage.parameters[1].gene as ObjectGene
        assertTrue(objPackage.fields.any { it is BooleanGene && it.name == "isMain" })
        assertTrue(objPackage.fields[2] is OptionalGene)
        val objVersion = (objPackage.fields[2] as OptionalGene).gene as ObjectGene
        objVersion.fields.any { it is BooleanGene && it.name == "name" }
        assertTrue(ppackage.parameters[1] is GQReturnParam)
        assertTrue(ppackage.parameters[1].gene is ObjectGene)
    }

    @Test
    fun reactFinlandSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/ReactFinland.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(12, actionCluster.size)

    }

    @Test
    fun travelgateXSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/TravelgateX.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(9, actionCluster.size)
        /**/
        val admin = actionCluster["admin"] as GraphQLAction
        assertEquals(1, admin.parameters.size)
        assertTrue(admin.parameters[0] is GQReturnParam)
        assertTrue(admin.parameters[0].gene is ObjectGene)
        /**/
        val hotelX = actionCluster["hotelX"] as GraphQLAction
        assertEquals(1, admin.parameters.size)
        assertTrue(hotelX.parameters[0] is GQReturnParam)
        /**/
        val logging = actionCluster["logging"] as GraphQLAction
        assertEquals(1, logging.parameters.size)
        assertTrue(logging.parameters[0] is GQReturnParam)
        assertTrue(logging.parameters[0].gene is ObjectGene)
    }


    @Test
    fun recEgTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/recEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)
    }

    @Test
    fun spaceXTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/SpaceX.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(43, actionCluster.size)

        val coresUpcoming = actionCluster["coresUpcoming"] as GraphQLAction
        assertEquals(6, coresUpcoming.parameters.size)
        assertTrue(coresUpcoming.parameters[0] is GQInputParam)
        assertTrue(coresUpcoming.parameters[1] is GQInputParam)
        assertTrue(coresUpcoming.parameters[2] is GQInputParam)
        assertTrue(coresUpcoming.parameters[5] is GQReturnParam)
        assertTrue((coresUpcoming.parameters[0].gene as OptionalGene).gene is ObjectGene)
        assertTrue(coresUpcoming.parameters[5].gene is ObjectGene)
        val objCore = coresUpcoming.parameters[5].gene as ObjectGene
        assertTrue(objCore.fields.any { it is BooleanGene && it.name == "water_landing" })

    }

    @Test
    fun bookTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Book.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(3, actionCluster.size)
    }


    @Test
    fun interfaceEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)


        val stores = actionCluster["stores"] as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val interfaceObjectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, interfaceObjectStore.fields.size)

        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objPotStore.fields.size)
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "address" })

        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, objStore.fields.size)
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

    }

    @Test
    fun interfaceEgFunctionTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceEgFunction.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val stores = actionCluster["stores"] as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val interfaceObjectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, interfaceObjectStore.fields.size)

        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objPotStore.fields.size)

        assertTrue(objPotStore.fields.any { it is TupleGene && it.name == "address" })

        val tupleAddress = objPotStore.fields.first { it.name == "address" } as TupleGene
        assertEquals(1, tupleAddress.elements.size)
        assertTrue(tupleAddress.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "y" })

        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, objStore.fields.size)
        assertTrue(objStore.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

        val tupleId = objStore.fields.first { it.name == "id" } as TupleGene

        assertEquals(1, tupleId.elements.size)
        assertTrue(tupleId.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "x" })

    }

    @Test
    fun interfaceInternalEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceInternalEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val stores = actionCluster["stores"] as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore1 = stores.parameters[0].gene as ObjectGene
        assertEquals(1, objectStore1.fields.size)

        assertTrue(objectStore1.fields[0] is OptionalGene)
        assertTrue((objectStore1.fields[0] as OptionalGene).gene is ObjectGene)
        val interfaceObjectStore = (objectStore1.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(2, interfaceObjectStore.fields.size)

        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objPotStore.fields.size)
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "address" })

        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, objStore.fields.size)
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

    }

    @Test
    fun unionInternalEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/unionInternalEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val stores = actionCluster["stores"] as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, objectStore.fields.size)
        assertTrue(objectStore.fields[0] is BooleanGene)
        assertTrue(objectStore.fields[1] is OptionalGene)
        assertTrue((objectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val unionObjBouquet = (objectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, unionObjBouquet.fields.size)
        assertTrue(unionObjBouquet.fields[0] is OptionalGene)
        assertTrue((unionObjBouquet.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlower = (unionObjBouquet.fields[0] as OptionalGene).gene as ObjectGene
        assertTrue(objFlower.name == "Flower")
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "color" })
        /**/
        assertTrue(unionObjBouquet.fields[1] is OptionalGene)
        assertTrue((unionObjBouquet.fields[1] as OptionalGene).gene is ObjectGene)
        val objPot = (unionObjBouquet.fields[1] as OptionalGene).gene as ObjectGene

        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "size" })

    }

    @Test
    fun unionInternalFunctionsEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/unionInternalFunctionsEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val stores = actionCluster["stores"] as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, objectStore.fields.size)
        assertTrue(objectStore.fields[0] is BooleanGene)
        assertTrue(objectStore.fields[1] is OptionalGene)
        assertTrue((objectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val unionObjBouquet = (objectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, unionObjBouquet.fields.size)

        assertTrue(unionObjBouquet.fields[0] is OptionalGene)
        assertTrue((unionObjBouquet.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlower = (unionObjBouquet.fields[0] as OptionalGene).gene as ObjectGene
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "color" })
        assertTrue(objFlower.fields.any { it is TupleGene && it.name == "name" })

        val tupleName = objFlower.fields.first { it.name == "name" } as TupleGene
        assertEquals(1, tupleName.elements.size)
        assertTrue(tupleName.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "x" })
        /**/
        assertTrue(unionObjBouquet.fields[1] is OptionalGene)
        assertTrue((unionObjBouquet.fields[1] as OptionalGene).gene is ObjectGene)
        val objPot = (unionObjBouquet.fields[1] as OptionalGene).gene as ObjectGene

        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "size" })
        assertTrue(objPot.fields.any { it is TupleGene && it.name == "color" })

        val tupleColor = objPot.fields.first { it.name == "color" } as TupleGene
        assertEquals(1, tupleColor.elements.size)
        assertTrue(tupleColor.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "y" })

    }

    @Test
    fun unionInternalRecEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/unionInternalRecEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val stores = actionCluster["stores"] as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, objectStore.fields.size)
        assertTrue(objectStore.fields[0] is BooleanGene)
        assertTrue(objectStore.fields[1] is OptionalGene)
        assertTrue((objectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val unionObjBouquet = (objectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, unionObjBouquet.fields.size)
        assertTrue(unionObjBouquet.fields[0] is OptionalGene)
        assertTrue((unionObjBouquet.fields[0] as OptionalGene).gene is ObjectGene)
        val objFlower = (unionObjBouquet.fields[0] as OptionalGene).gene as ObjectGene
        assertTrue(objFlower.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objFlower.fields.any { it is OptionalGene && it.name == "color" })
        /**/
        assertTrue(unionObjBouquet.fields[1] is OptionalGene)
        assertTrue((unionObjBouquet.fields[1] as OptionalGene).gene is ObjectGene)
        val objPot = (unionObjBouquet.fields[1] as OptionalGene).gene as ObjectGene

        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "size" })
    }

    @Test
    fun unionInternalRecEg2Test() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/unionInternalRecEg2.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)
    }


    @Test
    fun enumInterfaceTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/enumInterface.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

    }

    @Test
    fun interfaceHisTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceHis.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val node = actionCluster["node"] as GraphQLAction
        assertEquals(1, node.parameters.size)
        assertTrue(node.parameters[0] is GQReturnParam)

        assertTrue(node.parameters[0].gene is ObjectGene)
        val interfaceObjectNode = node.parameters[0].gene as ObjectGene
        assertEquals(5, interfaceObjectNode.fields.size)

        assertTrue(interfaceObjectNode.fields[0] is OptionalGene)
        assertTrue((interfaceObjectNode.fields[0] as OptionalGene).gene is ObjectGene)
        val objAgency = (interfaceObjectNode.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objAgency.fields.size)
        assertTrue(objAgency.fields.any { it is OptionalGene && it.name == "routes" })
    }


    @Test
    fun recEgTest2() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/recEg2.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)
    }

    @Test
    fun handleAllCyclesInObjectFieldsTest() {

        val objI = ObjectGene("obj2", listOf(OptionalGene("cyc", CycleObjectGene("a"), isActive = true)))

        val obj = OptionalGene("obj1", ObjectGene("obj1", listOf(objI)), isActive = true)

        assertTrue(obj.isActive)

        obj.flatView().forEach {
            if (it is ObjectGene)
                GraphQLActionBuilder.handleAllCyclesAndLimitInObjectFields(it)
        }

        assertTrue(!obj.isActive)

    }

    @Test
    fun handleAllCyclesXInObjectFieldsTest() {

        val objI = ObjectGene("obj2", listOf(OptionalGene("lim", LimitObjectGene("lim"), isActive = true)))

        val obj = OptionalGene("obj1", ObjectGene("obj1", listOf(objI)), isActive = true)

        assertTrue(obj.isActive)

        obj.flatView().forEach {
            if (it is ObjectGene)
                GraphQLActionBuilder.handleAllCyclesAndLimitInObjectFields(it)
        }

        assertTrue(!obj.isActive)

    }


    @Test
    fun depthTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/abstract2.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, max(config.treeDepth,5))
        assertEquals(2, actionCluster.size)

        val a = actionCluster["a"] as GraphQLAction
        assertEquals(1, a.parameters.size)
        assertTrue(a.parameters[0] is GQReturnParam)
        assertTrue(a.parameters[0].gene is ObjectGene)

        val objA = a.parameters[0].gene as ObjectGene//first level
        assertEquals(3, objA.fields.size)
        assertTrue(objA.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objA.fields.any { it is OptionalGene && it.name == "b" })// second level
        assertTrue(objA.fields.any { it is OptionalGene && it.name == "f" })// second level

        val objB = (objA.fields.first { it.name == "b" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objB.fields.size)
        assertTrue(objB.fields.any { it is OptionalGene && it.name == "c" })//third level

        val objC = (objB.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objC.fields.size)
        assertTrue(objC.fields.any { it is OptionalGene && it.name == "d" })//fourth level

        val objD = (objC.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(2, objD.fields.size)
        assertTrue(objD.fields.any { it is OptionalGene && it.name == "e" })//fifth level

        val objF = (objA.fields.first { it.name == "f" } as OptionalGene).gene as ObjectGene// second level
        assertEquals(1, objF.fields.size)

    }

    @Test
    fun functionInReturnedObjectsTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/functionInReturnedObjectsBase.json")
            .readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(2, actionCluster.size)
        val page2 = actionCluster["page2"] as GraphQLAction

        val page = actionCluster["page"] as GraphQLAction
        assertEquals(2, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue(page.parameters[1] is GQReturnParam)

        assertTrue(page.parameters[1].gene is ObjectGene)
        val objPage = page.parameters[1].gene as ObjectGene

        assertEquals(8, objPage.fields.size)
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo2" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo3" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users2" })
        assertTrue(objPage.fields.any { it is BooleanGene && it.name == "pageInfo4" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo5" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users3" })

        val objPageInfo = (objPage.fields.first { it.name == "pageInfo" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo.fields.size)
        assertTrue(objPageInfo.fields.any { it is BooleanGene && it.name == "total" })

        val objPageInfo2 = (objPage.fields.first { it.name == "pageInfo2" } as OptionalGene).gene as ObjectGene

        assertEquals(1, objPageInfo2.fields.size)
        assertTrue(objPageInfo2.fields.any { it is TupleGene && it.name == "total2" })
        val tupleTotal2 = objPageInfo2.fields.first { it.name == "total2" } as TupleGene
        assertEquals(1, tupleTotal2.elements.size)
        assertTrue(tupleTotal2.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "id" })

        val objPageInfo3 = (objPage.fields.first { it.name == "pageInfo3" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo3.fields.size)

        assertTrue(objPageInfo3.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "total3" })

        val objTotal3 = (objPageInfo3.fields.first { it.name == "total3" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objTotal3.fields.size)
        assertTrue(objTotal3.fields.any { it is TupleGene && it.name == "price" })

        val tuplePrice = objTotal3.fields.first { it.name == "price" } as TupleGene
        assertEquals(1, tuplePrice.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tuplePrice.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        /**/
        val tupleUsers2 = objPage.fields.first { it.name == "users2" } as TupleGene
        assertEquals(2, tupleUsers2.elements.size)
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search2" })
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "users2" })

        val objUser2 = (tupleUsers2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objUser2.fields.size)
        assertTrue(objUser2.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "about2" })
        /**/
        val objAbout2 = (objUser2.fields.first { it.name == "about2" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objAbout2.fields.size)
        assertTrue(objAbout2.fields.any { it is TupleGene && it.name == "html" })

        val tupleHtml = objAbout2.fields.first { it.name == "html" } as TupleGene
        assertEquals(1, tupleHtml.elements.size)
        assertTrue(tupleHtml.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        /**/
        val objPageInfo5 = (objPage.fields.first { it.name == "pageInfo5" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo5.fields.size)
        assertTrue(objPageInfo5.fields.any { it is BooleanGene && it.name == "total4" })
        /**/
        val tupleUsers3 = objPage.fields.first { it.name == "users3" } as TupleGene
        assertEquals(3, tupleUsers3.elements.size)

        assertTrue(tupleUsers3.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search" })
        assertTrue(tupleUsers3.elements.any { it is ArrayGene<*> && it.template is ObjectGene && it.name == "store" })

        val objStore = (tupleUsers3.elements.first { it.name == "store" } as ArrayGene<*>).template as ObjectGene
        assertEquals(1, objStore.fields.size)
        assertTrue(objStore.fields.any { it is IntegerGene && it.name == "id" })

        assertTrue(tupleUsers3.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "users3" })

        val objUser3 = (tupleUsers3.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objUser3.fields.size)
        assertTrue(objUser3.fields.any { it is TupleGene && it.name == "about3" })

        val tupleAbout3 = objUser3.fields.first { it.name == "about3" } as TupleGene
        assertEquals(1, tupleAbout3.elements.size)
        assertTrue(tupleAbout3.elements.any { it is OptionalGene && it.gene is BooleanGene && it.name == "AsHtml2" })
    }

    /*
    The test underneath are for testing schemas without the boolean selection.
    It helps when investigating the structure of each component, and Gc error
     */
    @Disabled
    @Test
    fun functionInReturnedObjectsWithOutBooleanSelectionTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/functionInReturnedObjectsBase.json")
            .readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(2, actionCluster.size)
        val page = actionCluster.get("page") as GraphQLAction
        assertEquals(2, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue(page.parameters[1] is GQReturnParam)

        assertTrue((page.parameters[1].gene as OptionalGene).gene is ObjectGene)
        val objPage = (page.parameters[1].gene as OptionalGene).gene as ObjectGene

        assertEquals(8, objPage.fields.size)
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "users" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo2" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo3" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "users2" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is StringGene && it.name == "pageInfo4" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo5" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "users3" })

        val objPageInfo = (objPage.fields.first { it.name == "pageInfo" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo.fields.size)
        assertTrue(objPageInfo.fields.any { it is OptionalGene && it.gene is IntegerGene && it.name == "total" })

        val objPageInfo2 = (objPage.fields.first { it.name == "pageInfo2" } as OptionalGene).gene as ObjectGene

        assertEquals(1, objPageInfo2.fields.size)
        assertTrue(objPageInfo2.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "total2" })
        val tupleTotal2 = (objPageInfo2.fields.first { it.name == "total2" } as OptionalGene).gene as TupleGene
        assertEquals(2, tupleTotal2.elements.size)
        assertTrue(tupleTotal2.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "id" })
        assertTrue(tupleTotal2.elements.any { it is OptionalGene && it.gene is BooleanGene && it.name == "total2" })

        val objPageInfo3 = (objPage.fields.first { it.name == "pageInfo3" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo3.fields.size)

        assertTrue(objPageInfo3.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "total3" })

        val objTotal3 = (objPageInfo3.fields.first { it.name == "total3" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objTotal3.fields.size)
        assertTrue(objTotal3.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "price" })

        val tuplePrice = (objTotal3.fields.first { it.name == "price" } as OptionalGene).gene as TupleGene
        assertEquals(2, tuplePrice.elements.size)
        assertTrue(tuplePrice.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        assertTrue(tuplePrice.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "price" })
        /**/
        val tupleUsers2 = (objPage.fields.first { it.name == "users2" } as OptionalGene).gene as TupleGene
        assertEquals(2, tupleUsers2.elements.size)
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search2" })
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "users2" })

        val objUser2 = (tupleUsers2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objUser2.fields.size)
        assertTrue(objUser2.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "about2" })
        /**/
        val objAbout2 = (objUser2.fields.first { it.name == "about2" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objAbout2.fields.size)
        assertTrue(objAbout2.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "html" })

        val tupleHtml = (objAbout2.fields.first { it.name == "html" } as OptionalGene).gene as TupleGene
        assertEquals(2, tupleHtml.elements.size)
        assertTrue(tupleHtml.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        assertTrue(tupleHtml.elements.any { it is OptionalGene && it.gene is BooleanGene && it.name == "html" })
        /**/
        val objPageInfo5 = (objPage.fields.first { it.name == "pageInfo5" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo5.fields.size)
        assertTrue(objPageInfo5.fields.any { it is OptionalGene && it.gene is EnumGene<*> && it.name == "total4" })

        val enumTotal4 = (objPageInfo5.fields.first { it.name == "total4" } as OptionalGene).gene as EnumGene<*>
        assertEquals(2, enumTotal4.values.size)
        assertTrue(enumTotal4.values.any { it == "TOTALENUM1" })
        assertTrue(enumTotal4.values.any { it == "TOTALENUM2" })
        /**/
        val tupleUsers3 = (objPage.fields.first { it.name == "users3" } as OptionalGene).gene as TupleGene
        assertEquals(3, tupleUsers3.elements.size)
        assertTrue(tupleUsers3.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search" })
        assertTrue(tupleUsers3.elements.any { it is ArrayGene<*> && it.template is ObjectGene && it.name == "store" })
        val objStore = (tupleUsers3.elements.first { it.name == "store" } as ArrayGene<*>).template as ObjectGene
        assertEquals(1, objStore.fields.size)
        assertTrue(objStore.fields.any { it is IntegerGene && it.name == "id" })
    }


    @Test
    fun anigListSchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/AniList.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(54, actionCluster.size)
        val page = actionCluster["Page"] as GraphQLAction
        assertEquals(3, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue((page.parameters[1].gene as OptionalGene).gene is IntegerGene)

        assertTrue(page.parameters[1] is GQInputParam)
        assertTrue(page.parameters[2] is GQReturnParam)

        //primitive type that is not part of the search
        val genreCollection = actionCluster["GenreCollection"] as GraphQLAction

        val mediaTagCollection = actionCluster["MediaTagCollection"] as GraphQLAction
        assertTrue(mediaTagCollection.parameters[1].gene is ObjectGene)

        val objPage = page.parameters[2].gene as ObjectGene
        assertTrue(objPage.fields[0] is OptionalGene)
        val objPageInfo = (objPage.fields[0] as OptionalGene).gene as ObjectGene
        objPageInfo.fields.any { it is BooleanGene && it.name == "Total" }
        assertTrue(objPageInfo.fields[0] is BooleanGene)
        /**/
        val media = actionCluster["Media"] as GraphQLAction
        assertEquals(67, media.parameters.size)
        assertTrue((media.parameters[6].gene as OptionalGene).gene is EnumGene<*>)

        val objMedia = media.parameters[66].gene as ObjectGene
        assertTrue(objMedia.fields.any { it is BooleanGene && it.name == "type" })
        /**/
        val notification = actionCluster["Notification"] as GraphQLAction
        assertEquals(4, notification.parameters.size)
        assertTrue(notification.parameters[0] is GQInputParam)
        assertTrue(notification.parameters[3] is GQReturnParam)


        assertTrue(notification.parameters[3].gene is ObjectGene)
        val unionObjectsNotificationUnion = notification.parameters[3].gene as ObjectGene
        assertEquals(14, unionObjectsNotificationUnion.fields.size)

        assertTrue(unionObjectsNotificationUnion.fields[0] is OptionalGene)
        assertTrue((unionObjectsNotificationUnion.fields[0] as OptionalGene).gene is ObjectGene)
        val objAiringNotification = (unionObjectsNotificationUnion.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(7, objAiringNotification.fields.size)
        assertTrue(objAiringNotification.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objAiringNotification.fields.any { it is OptionalGene && it.name == "media" })


        val objMediaa = (objAiringNotification.fields.first { it.name == "media" } as OptionalGene).gene as ObjectGene
        assertEquals(53, objMediaa.fields.size)
        assertTrue(objMediaa.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objMediaa.fields.any { it is BooleanGene && it.name == "modNotes" })

    }

    @Test
    fun gitLabSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/GitLab.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(169, actionCluster.size)

    }


    @Test
    fun universeSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Universe.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(87, actionCluster.size)
    }

    @Test
    fun historyInFunctionInReturnedObject() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/HistoryInFunctionInReturnedObject.json")
            .readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(2, actionCluster.size)

        val page = actionCluster["page"] as GraphQLAction
        assertEquals(2, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue(page.parameters[1] is GQReturnParam)

        assertTrue(page.parameters[1].gene is ObjectGene)
        val objPage = page.parameters[1].gene as ObjectGene

        assertEquals(1, objPage.fields.size)
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users" })

        val tupleUsers = objPage.fields.first { it.name == "users" } as TupleGene
        assertEquals(2, tupleUsers.elements.size)
        assertTrue(tupleUsers.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search" })

        val objUser = (tupleUsers.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objUser.fields.size)
        assertTrue(objUser.fields.any { it is TupleGene && it.name == "about" })

        val tupleAbout = objUser.fields.first { it.name == "about" } as TupleGene
        assertEquals(1, tupleAbout.elements.size)
        assertTrue(tupleAbout.elements.any { it is OptionalGene && it.gene is BooleanGene && it.name == "AsHtml" })
        /**/
        val pageInfo = actionCluster["pageInfo"] as GraphQLAction
        assertEquals(1, pageInfo.parameters.size)
        assertTrue(pageInfo.parameters[0] is GQReturnParam)

        assertTrue(pageInfo.parameters[0].gene is ObjectGene)
        val objPageInfo = pageInfo.parameters[0].gene as ObjectGene

        assertEquals(2, objPageInfo.fields.size)
        assertTrue(objPageInfo.fields.any { it is TupleGene && it.name == "total" })

        val tupleTotal = objPageInfo.fields.first { it.name == "total" } as TupleGene
        assertEquals(2, tupleTotal.elements.size)
        assertTrue(tupleTotal.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "id" })
        assertTrue((tupleTotal.elements.last() as OptionalGene).gene is ObjectGene)

        /**/
        assertTrue(objPageInfo.fields.any { it is TupleGene && it.name == "total2" })
    }

    @Test
    fun timbuctooSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Timbuctoo.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(19, actionCluster.size)

        val aboutMe = actionCluster["aboutMe"] as GraphQLAction
        assertEquals(1, aboutMe.parameters.size)
        assertTrue(aboutMe.parameters[0] is GQReturnParam)

        assertTrue(aboutMe.parameters[0].gene is ObjectGene)
        val objAboutMe = aboutMe.parameters[0].gene as ObjectGene

        assertEquals(6, objAboutMe.fields.size)
        assertTrue(objAboutMe.fields.any { it is OptionalGene && it.gene is ObjectGene && it.name == "dataSets" })
        assertTrue(objAboutMe.fields.any { it is TupleGene && it.name == "dataSetMetadataList" })

        val tupleDataSetMetadataList = objAboutMe.fields.first { it.name == "dataSetMetadataList" } as TupleGene
        assertEquals(5, tupleDataSetMetadataList.elements.size)
        assertTrue((tupleDataSetMetadataList.elements.last() as OptionalGene).gene !is CycleObjectGene)
    }

    @Disabled("this gives lot of GC issues")
    @Test
    fun zoraTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Zora.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(85, actionCluster.size)
    }

    @Test
    fun faunaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Fauna.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(13, actionCluster.size)
    }

    @Test
    fun rootNameTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/RootNames.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(2, actionCluster.size)
    }

    @Disabled
    @Test
    fun gitHubTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/GitHub.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(204, actionCluster.size)
    }

}