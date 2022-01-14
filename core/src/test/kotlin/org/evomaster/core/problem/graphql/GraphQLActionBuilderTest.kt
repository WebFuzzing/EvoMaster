package org.evomaster.core.problem.graphql

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class GraphQLActionBuilderTest {


    @Test
    fun testPetClinic() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/PetsClinic.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(15, actionCluster.size)

        val pettypes = actionCluster.get("pettypes") as GraphQLAction
        assertEquals(1, pettypes.parameters.size)
        assertTrue(pettypes.parameters[0] is GQReturnParam)
        assertTrue(pettypes.parameters[0].gene is ObjectGene)
        val objPetType = pettypes.parameters[0].gene as ObjectGene
        assertEquals(2, objPetType.fields.size)

        assertTrue(objPetType.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(objPetType.fields.any { it is TupleGene && it.name == "name" })

        val tupleId = objPetType.fields.first { it.name == "id" } as TupleGene
        assertEquals(1, tupleId.elements.size)
        assertTrue(tupleId.elements.any { it is BooleanGene && it.name == "id" })

        val tupleName = objPetType.fields.first { it.name == "name" } as TupleGene
        assertEquals(1, tupleName.elements.size)
        assertTrue(tupleName.elements.any { it is BooleanGene && it.name == "name" })

        //TODO copy function
      //  val gQlReturn = GQReturnParam(pettypes.parameters[0].name, pettypes.parameters[0].gene)
        //val gQlInputcopy = gQlReturn.copy()
        //assertEquals(gQlReturn.name, gQlInputcopy.name)
        //assertEquals(gQlReturn.gene.name, gQlInputcopy.gene.name)
        /**/
        val vets = actionCluster.get("vets") as GraphQLAction
        assertEquals(1, vets.parameters.size)
        assertTrue(vets.parameters[0] is GQReturnParam)
        assertTrue(vets.parameters[0].gene is ObjectGene)
        val objVets = vets.parameters[0].gene as ObjectGene
        assertEquals(4, objVets.fields.size)

        assertTrue(objVets.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(objVets.fields.any { it is TupleGene && it.name == "firstName" })
        assertTrue(objVets.fields.any { it is TupleGene && it.name == "lastName" })
        assertTrue(objVets.fields.any { it is TupleGene && it.name == "specialties" })

        val tupleIdVets = objVets.fields.first { it.name == "id" } as TupleGene
        assertEquals(1, tupleIdVets.elements.size)
        assertTrue(tupleIdVets.elements.any { it is BooleanGene && it.name == "id" })

        val tupleFirstNameVets = objVets.fields.first { it.name == "firstName" } as TupleGene
        assertEquals(1, tupleFirstNameVets.elements.size)
        assertTrue(tupleFirstNameVets.elements.any { it is BooleanGene && it.name == "firstName" })

        val tupleLastNameVets = objVets.fields.first { it.name == "lastName" } as TupleGene
        assertEquals(1, tupleLastNameVets.elements.size)
        assertTrue(tupleLastNameVets.elements.any { it is BooleanGene && it.name == "lastName" })

        val tupleSpecialties = objVets.fields.first { it.name == "specialties" } as TupleGene
        assertEquals(1, tupleSpecialties.elements.size)
        assertTrue(tupleSpecialties.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "specialties" })

        val objSpecialties = (tupleSpecialties.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(2, objSpecialties.fields.size)
        assertTrue(objSpecialties.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(objSpecialties.fields.any { it is TupleGene && it.name == "name" })

        /**/
        val owners = actionCluster.get("owners") as GraphQLAction
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

        assertTrue(owner.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(owner.fields.any { it is TupleGene && it.name == "firstName" })
        assertTrue(owner.fields.any { it is TupleGene && it.name == "lastName" })
        assertTrue(owner.fields.any { it is TupleGene && it.name == "address" })
        assertTrue(owner.fields.any { it is TupleGene && it.name == "city" })
        assertTrue(owner.fields.any { it is TupleGene && it.name == "telephone" })
        assertTrue(owner.fields.any { it is TupleGene && it.name == "pets" })

        val tupleIdOwner = owner.fields.first { it.name == "id" } as TupleGene
        assertEquals(1, tupleIdOwner.elements.size)
        assertTrue(tupleIdOwner.elements.any { it is BooleanGene && it.name == "id" })

        val tupleFirstNameOwner = owner.fields.first { it.name == "firstName" } as TupleGene
        assertEquals(1, tupleFirstNameOwner.elements.size)
        assertTrue(tupleFirstNameOwner.elements.any { it is BooleanGene && it.name == "firstName" })

        val tupleLastNameOwner = owner.fields.first { it.name == "lastName" } as TupleGene
        assertEquals(1, tupleLastNameOwner.elements.size)
        assertTrue(tupleLastNameOwner.elements.any { it is BooleanGene && it.name == "lastName" })

        val tuplePets = owner.fields.first { it.name == "pets" } as TupleGene
        assertEquals(1, tuplePets.elements.size)
        assertTrue(tuplePets.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pets" })

        val objPets = (tuplePets.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(6, objPets.fields.size)
        assertTrue(objPets.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(objPets.fields.any { it is TupleGene && it.name == "name" })
        assertTrue(objPets.fields.any { it is TupleGene && it.name == "birthDate" })
        assertTrue(objPets.fields.any { it is TupleGene && it.name == "type" })
        assertTrue(objPets.fields.any { it is TupleGene && it.name == "visits" })

        val tupleVisits = objPets.fields.first { it.name == "visits" } as TupleGene
        assertEquals(1, tupleVisits.elements.size)
        assertTrue(tupleVisits.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "visits" })

        val objVisits = (tupleVisits.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(2, objVisits.fields.size)
        assertTrue(objVisits.fields.any { it is TupleGene && it.name == "visits" })
        assertTrue(objVisits.fields.any { it is TupleGene && it.name == "totalCount" })

  //todo

//        GeneUtils.repairBooleanSelection(owner); // this should not fail
        /**/
        val pet = actionCluster.get("pet") as GraphQLAction
        assertEquals(2, pet.parameters.size)
        assertTrue(pet.parameters[0] is GQInputParam)
        assertTrue(pet.parameters[0].gene is IntegerGene)
        assertTrue(pet.parameters[0].gene.name == "id")
        assertTrue(pet.parameters[1] is GQReturnParam)
        assertTrue(pet.parameters[1].gene is ObjectGene)
        val objPet2 = (pet.parameters[1].gene as ObjectGene)
        assertEquals(6, objPet2.fields.size)

        assertTrue(objPet2.fields.any { it is TupleGene && it.name == "visits" })

        val tupleVisits2 = objPet2.fields.first { it.name == "visits" } as TupleGene
        assertEquals(1, tupleVisits2.elements.size)
        assertTrue(tupleVisits2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "visits" })

        //todo
        //GeneUtils.repairBooleanSelection(objPet2); // this should not fail
        /**/
        val specialties = actionCluster.get("specialties") as GraphQLAction
        assertEquals(1, specialties.parameters.size)
        assertTrue(specialties.parameters[0] is GQReturnParam)
        assertTrue(specialties.parameters[0].gene is ObjectGene)

    }

    @Disabled
    @Test
    fun anigListSchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/AniList.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(54, actionCluster.size)
        val page = actionCluster.get("Page") as GraphQLAction
        assertEquals(3, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue((page.parameters[1].gene as OptionalGene).gene is IntegerGene)

        assertTrue(page.parameters[1] is GQInputParam)
        assertTrue(page.parameters[2] is GQReturnParam)

        //primitive type that is not part of the search
        val genreCollection = actionCluster.get("GenreCollection") as GraphQLAction

        val mediaTagCollection = actionCluster.get("MediaTagCollection") as GraphQLAction
        assertTrue(mediaTagCollection.parameters[1].gene is ObjectGene)

        val objPage = page.parameters[2].gene as ObjectGene
        assertTrue(objPage.fields[0] is OptionalGene)
        val objPageInfo = (objPage.fields[0] as OptionalGene).gene as ObjectGene
        objPageInfo.fields.any({ it is BooleanGene && it.name == "Total" })
        assertTrue(objPageInfo.fields[0] is BooleanGene)
        /**/
        val media = actionCluster.get("Media") as GraphQLAction
        assertEquals(67, media.parameters.size)
        assertTrue((media.parameters[6].gene as OptionalGene).gene is EnumGene<*>)

        val objMedia = media.parameters[66].gene as ObjectGene
        assertTrue(objMedia.fields.any { it is BooleanGene && it.name == "type" })
        /**/
        val notification = actionCluster.get("Notification") as GraphQLAction
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

    @Disabled
    @Test
    fun bitquerySchemaTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Bitquery.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(12, actionCluster.size)

        val algorand = actionCluster.get("algorand") as GraphQLAction
        assertEquals(2, algorand.parameters.size)
        assertTrue(algorand.parameters[0] is GQInputParam)
        assertTrue(algorand.parameters[1] is GQReturnParam)
        assertTrue(algorand.parameters[1].gene is ObjectGene)
        val objAlgorand = algorand.parameters[1].gene as ObjectGene
        assertEquals(7, objAlgorand.fields.size)
        assertTrue(objAlgorand.fields.any { it is OptionalGene && it.name == "address" })

    }

    @Disabled
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

        val asset = actionCluster.get("asset") as GraphQLAction
        assertEquals(4, asset.parameters.size)
        assertTrue(asset.parameters[0] is GQInputParam)
        assertTrue(asset.parameters[1] is GQInputParam)
        assertTrue(asset.parameters[2] is GQInputParam)
        assertTrue(asset.parameters[3] is GQReturnParam)
        assertTrue(asset.parameters[0].gene is StringGene)
        assertTrue((asset.parameters[1].gene as OptionalGene).gene is BooleanGene)
        assertTrue(asset.parameters[3].gene is ObjectGene)
        /**/
        val categoryCollection = actionCluster.get("categoryCollection") as GraphQLAction
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

        val continents = actionCluster.get("continents") as GraphQLAction
        assertEquals(2, continents.parameters.size)
        assertTrue(continents.parameters[0] is GQInputParam)
        assertTrue(continents.parameters[1] is GQReturnParam)
        assertTrue(continents.parameters[1].gene is ObjectGene)

        val objContinents = continents.parameters[1].gene as ObjectGene
        assertTrue(objContinents.fields[2] is TupleGene)

        val tupleCountry = objContinents.fields[2] as TupleGene
        assertTrue(tupleCountry.elements.any { it is OptionalGene })
    }

    @Test
    fun deutscheBahnSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/DeutscheBahn.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(7, actionCluster.size)

        val routing = actionCluster.get("routing") as GraphQLAction
        assertEquals(3, routing.parameters.size)
        assertTrue(routing.parameters[0] is GQInputParam)
        assertTrue(routing.parameters[2] is GQReturnParam)
        assertTrue(routing.parameters[2].gene is ObjectGene)

    }

    @Disabled
    @Test
    fun digitransitHSLSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/DigitransitHSL.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(33, actionCluster.size)

        val node = actionCluster.get("node") as GraphQLAction
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

    @Disabled
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

    @Disabled
    @Test
    fun gitLabSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/GitLab.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(169, actionCluster.size)

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

        val ppackage = actionCluster.get("package") as GraphQLAction
        assertEquals(2, ppackage.parameters.size)
        assertTrue(ppackage.parameters[0] is GQInputParam)
        assertTrue(ppackage.parameters[0].gene is StringGene)
        assertTrue(ppackage.parameters[1] is GQReturnParam)
        assertTrue(ppackage.parameters[1].gene is ObjectGene)

        val objPackage = ppackage.parameters[1].gene as ObjectGene
        assertTrue(objPackage.fields.any { it is TupleGene && it.name == "isMain" })
        assertTrue(objPackage.fields.any { it is TupleGene && it.name == "version" })

        val tupleIsMain = objPackage.fields.first { it.name == "isMain" } as TupleGene
        assertTrue(tupleIsMain.elements.any { it is BooleanGene && it.name == "isMain" })

        val tupleVersion = objPackage.fields.first { it.name == "version" } as TupleGene
        assertTrue(tupleVersion.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "version" })

        val objVersion = (tupleVersion.elements.last() as OptionalGene).gene as ObjectGene
        assertTrue(objVersion.fields.any { it is TupleGene && it.name == "name" })

        val tupleName = objVersion.fields.first { it.name == "name" } as TupleGene
        assertEquals(1, tupleName.elements.size)
        assertTrue(tupleName.elements.any { it is BooleanGene && it.name == "name" })

    }

    @Test
    fun reactFinlandSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/ReactFinland.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(12, actionCluster.size)

    }

    @Disabled
    @Test
    fun travelgateXSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/TravelgateX.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(9, actionCluster.size)
        /**/
        val admin = actionCluster.get("admin") as GraphQLAction
        assertEquals(1, admin.parameters.size)
        assertTrue(admin.parameters[0] is GQReturnParam)
        assertTrue(admin.parameters[0].gene is ObjectGene)
        /**/
        val hotelX = actionCluster.get("hotelX") as GraphQLAction
        assertEquals(1, admin.parameters.size)
        assertTrue(hotelX.parameters[0] is GQReturnParam)
        /**/
        val logging = actionCluster.get("logging") as GraphQLAction
        assertEquals(1, logging.parameters.size)
        assertTrue(logging.parameters[0] is GQReturnParam)
        assertTrue(logging.parameters[0].gene is ObjectGene)
    }

    @Disabled
    @Test
    fun universeSchemaTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Universe.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(87, actionCluster.size)
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

        val coresUpcoming = actionCluster.get("coresUpcoming") as GraphQLAction
        assertEquals(6, coresUpcoming.parameters.size)
        assertTrue(coresUpcoming.parameters[0] is GQInputParam)
        assertTrue(coresUpcoming.parameters[1] is GQInputParam)
        assertTrue(coresUpcoming.parameters[2] is GQInputParam)
        assertTrue(coresUpcoming.parameters[5] is GQReturnParam)
        assertTrue((coresUpcoming.parameters[0].gene as OptionalGene).gene is ObjectGene)
        assertTrue(coresUpcoming.parameters[5].gene is ObjectGene)
        val objCore = coresUpcoming.parameters[5].gene as ObjectGene
        assertTrue(objCore.fields.any { it is TupleGene && it.name == "water_landing" })

        val tupleWaterLanding = objCore.fields.first { it.name == "water_landing" } as TupleGene
        assertTrue(tupleWaterLanding.elements.any { it is BooleanGene && it.name == "water_landing" })

    }

    @Test
    fun bookTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/Book.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(3, actionCluster.size)
    }


    @Disabled
    @Test
    fun interfaceEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val interfaceObjectStore = stores.parameters[0].gene as ObjectGene
        assertEquals(2, interfaceObjectStore.fields.size)

        // basic interface not removed and object gene without fields removed
        // assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        // assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        // val objFlowerStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        // assertEquals(0, objFlowerStore.fields.size)
        // assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "id" })
        // assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "name" })

        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objPotStore.fields.size)
        // assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "id" })
        // assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "name" })
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "address" })

        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, objStore.fields.size)
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

    }

    @Disabled
    @Test
    fun interfaceInternalEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceInternalEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
        assertEquals(1, stores.parameters.size)
        assertTrue(stores.parameters[0] is GQReturnParam)

        assertTrue(stores.parameters[0].gene is ObjectGene)
        val objectStore1 = stores.parameters[0].gene as ObjectGene
        assertEquals(1, objectStore1.fields.size)


        assertTrue(objectStore1.fields[0] is OptionalGene)
        assertTrue((objectStore1.fields[0] as OptionalGene).gene is ObjectGene)
        val interfaceObjectStore = (objectStore1.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(2, interfaceObjectStore.fields.size)
        // assertEquals(2, interfaceObjectStore.fields.size)

        // assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        // assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        // val objFlowerStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        // assertEquals(0, objFlowerStore.fields.size)
        // assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "id" })
        // assertTrue(objFlowerStore.fields.any { it is BooleanGene && it.name == "name" })


        assertTrue(interfaceObjectStore.fields[0] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[0] as OptionalGene).gene is ObjectGene)
        val objPotStore = (interfaceObjectStore.fields[0] as OptionalGene).gene as ObjectGene
        assertEquals(1, objPotStore.fields.size)
        // assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "id" })
        // assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "name" })
        assertTrue(objPotStore.fields.any { it is BooleanGene && it.name == "address" })

        assertTrue(interfaceObjectStore.fields[1] is OptionalGene)
        assertTrue((interfaceObjectStore.fields[1] as OptionalGene).gene is ObjectGene)
        val objStore = (interfaceObjectStore.fields[1] as OptionalGene).gene as ObjectGene
        assertEquals(2, objStore.fields.size)
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objStore.fields.any { it is BooleanGene && it.name == "name" })

    }

    @Disabled
    @Test
    fun unionInternalEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/unionInternalEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
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

        /**/
        assertTrue(unionObjBouquet.fields[1] is OptionalGene)
        assertTrue((unionObjBouquet.fields[1] as OptionalGene).gene is ObjectGene)
        val objPot = (unionObjBouquet.fields[1] as OptionalGene).gene as ObjectGene

        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "id" })
        assertTrue(objPot.fields.any { it is BooleanGene && it.name == "size" })

    }

    @Disabled
    @Test
    fun unionInternalRecEgTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/unionInternalRecEg.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)


        val stores = actionCluster.get("stores") as GraphQLAction
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

    @Disabled
    @Test
    fun interfaceHisTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/interfaceHis.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)

        val node = actionCluster.get("node") as GraphQLAction
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
                GraphQLActionBuilder.handleAllCyclesInObjectFields(it)
        }

        assertTrue(!obj.isActive)

    }


    @Test
    fun depthTest() {
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/abstract2.json").readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)
        assertEquals(2, actionCluster.size)

        val a = actionCluster.get("a") as GraphQLAction
        assertEquals(1, a.parameters.size)
        assertTrue(a.parameters[0] is GQReturnParam)
        assertTrue(a.parameters[0].gene is ObjectGene)

        val objA = a.parameters[0].gene as ObjectGene
        assertEquals(3, objA.fields.size)

        assertTrue(objA.fields.any { it is TupleGene && it.name == "id" })
        assertTrue(objA.fields.any { it is TupleGene && it.name == "b" })
        assertTrue(objA.fields.any { it is TupleGene && it.name == "f" })

        val tupleId = objA.fields.first { it.name == "id" } as TupleGene
        assertEquals(1, tupleId.elements.size)
        assertTrue(tupleId.elements.any { it is BooleanGene && it.name == "id" })

        val tupleB = objA.fields.first { it.name == "b" } as TupleGene
        assertEquals(1, tupleB.elements.size)
        assertTrue(tupleB.elements.any { it is OptionalGene && it.name == "b" })

        val tupleF = objA.fields.first { it.name == "f" } as TupleGene
        assertEquals(1, tupleF.elements.size)
        assertTrue(tupleF.elements.any { it is OptionalGene && it.name == "f" })

        val objB = (tupleB.elements.first { it.name == "b" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objB.fields.size)
        assertTrue(objB.fields.any { it is TupleGene && it.name == "c" })

        val tupleC = objB.fields.first { it.name == "c" } as TupleGene
        assertEquals(1, tupleC.elements.size)
        assertTrue(tupleC.elements.any { it is OptionalGene && it.name == "c" })

        val objC = (tupleC.elements.first { it.name == "c" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objC.fields.size)
        assertTrue(objC.fields.any { it is TupleGene && it.name == "d" })

        val tupleD = objC.fields.first { it.name == "d" } as TupleGene
        assertEquals(1, tupleD.elements.size)
        assertTrue(tupleD.elements.any { it is OptionalGene && it.name == "d" })

        val objD = (tupleD.elements.first { it.name == "d" } as OptionalGene).gene as ObjectGene
        assertEquals(2, objD.fields.size)
        assertTrue(objD.fields.any { it is TupleGene && it.name == "e" })

        val objF = (tupleF.elements.first { it.name == "f" } as OptionalGene).gene as ObjectGene
        assertEquals(1, objF.fields.size)

    }

    //@Disabled
    @Test
    fun functionInReturnedObjectsTest() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/anilist(Fragment1PageInfo3Users2).json")
            .readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)
        val page = actionCluster.get("page") as GraphQLAction
        assertEquals(2, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue(page.parameters[1] is GQReturnParam)

        assertTrue(page.parameters[1].gene is ObjectGene)
        val objPage = page.parameters[1].gene as ObjectGene

        assertEquals(8, objPage.fields.size)
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "pageInfo" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "pageInfo2" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "pageInfo3" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users2" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "pageInfo4" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "pageInfo5" })
        assertTrue(objPage.fields.any { it is TupleGene && it.name == "users3" })

        val tuplePageInfo = objPage.fields.first { it.name == "pageInfo" } as TupleGene
        assertEquals(1, tuplePageInfo.elements.size)
        assertTrue(tuplePageInfo.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo" })

        val objPageInfo = (tuplePageInfo.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo.fields.size)
        assertTrue(objPageInfo.fields.any { it is TupleGene && it.name == "total" })
        val tupleTotal = objPageInfo.fields.first { it.name == "total" } as TupleGene
        assertEquals(1, tupleTotal.elements.size)
        assertTrue(tupleTotal.elements.any { it is BooleanGene && it.name == "total" })

        val tuplePageInfo2 = objPage.fields.first { it.name == "pageInfo2" } as TupleGene
        assertEquals(1, tuplePageInfo2.elements.size)
        assertTrue(tuplePageInfo2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo2" })

        val objPageInfo2 = (tuplePageInfo2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo2.fields.size)
        assertTrue(objPageInfo2.fields.any { it is TupleGene && it.name == "total2" })
        val tupleTotal2 = objPageInfo2.fields.first { it.name == "total2" } as TupleGene
        assertEquals(2, tupleTotal2.elements.size)
        assertTrue(tupleTotal2.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "id" })
        assertTrue(tupleTotal2.elements.any { it is BooleanGene && it.name == "total2" })

        val tuplePageInfo3 = objPage.fields.first { it.name == "pageInfo3" } as TupleGene
        assertEquals(1, tuplePageInfo3.elements.size)
        assertTrue(tuplePageInfo3.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo3" })

        val objPageInfo3 = (tuplePageInfo3.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo3.fields.size)
        assertTrue(objPageInfo3.fields.any { it is TupleGene && it.name == "total3" })
        val tupleTotal3 = objPageInfo3.fields.first { it.name == "total3" } as TupleGene
        assertEquals(1, tupleTotal3.elements.size)
        assertTrue(tupleTotal3.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "total3" })

        val objTotal3 = (tupleTotal3.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objTotal3.fields.size)
        assertTrue(objTotal3.fields.any { it is TupleGene && it.name == "price" })

        val tuplePrice = objTotal3.fields.first { it.name == "price" } as TupleGene
        assertEquals(2, tuplePrice.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tuplePrice.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        assertTrue(tuplePrice.elements.any { it is BooleanGene && it.name == "price" })

        val tupleUsers2 = objPage.fields.first { it.name == "users2" } as TupleGene
        assertEquals(2, tupleUsers2.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search2" })
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "users2" })

        val objUser2 = (tupleUsers2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objUser2.fields.size)
        assertTrue(objUser2.fields.any { it is TupleGene && it.name == "about2" })

        val tupleAbout2 = objUser2.fields.first { it.name == "about2" } as TupleGene
        assertEquals(1, tupleAbout2.elements.size)
        assertTrue(tupleAbout2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "about2" })


        val objAbout2 = (tupleAbout2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objAbout2.fields.size)
        assertTrue(objAbout2.fields.any { it is TupleGene && it.name == "html" })

        val tupleHtml = objAbout2.fields.first { it.name == "html" } as TupleGene
        assertEquals(2, tupleHtml.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tupleHtml.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        assertTrue(tupleHtml.elements.any { it is BooleanGene && it.name == "html" })


        val tuplePageInfo4 = objPage.fields.first { it.name == "pageInfo4" } as TupleGene
        assertEquals(1, tuplePageInfo4.elements.size)
        assertTrue(tuplePageInfo4.elements.any { it is BooleanGene && it.name == "pageInfo4" })


        val tuplePageInfo5 = objPage.fields.first { it.name == "pageInfo5" } as TupleGene
        assertEquals(1, tuplePageInfo5.elements.size)
        assertTrue(tuplePageInfo5.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo5" })

        val objPageInfo5 = (tuplePageInfo5.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo5.fields.size)
        assertTrue(objPageInfo5.fields.any { it is TupleGene && it.name == "total4" })

        val tupleTotal4 = objPageInfo5.fields.first { it.name == "total4" } as TupleGene
        assertEquals(1, tupleTotal4.elements.size)
        assertTrue(tupleTotal4.elements.any { it is BooleanGene && it.name == "total4" })


        val tupleUsers3 = objPage.fields.first { it.name == "users3" } as TupleGene
        assertEquals(3, tupleUsers3.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tupleUsers3.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search" })
        assertTrue(tupleUsers3.elements.any { it is ArrayGene<*> && it.template is ObjectGene && it.name == "store" })
        val objStore = (tupleUsers3.elements.first { it.name == "store" } as ArrayGene<*>).template as ObjectGene
        assertEquals(1, objStore.fields.size)
        assertTrue(objStore.fields.any { it is IntegerGene && it.name == "id" })

    }

    /*
    The test underneath are for testing schemas without the boolean selection.
    It helps when investigating the structure of each component
     */
    @Disabled
    @Test
    fun functionInReturnedObjectsWithOutBooleanSelectionTest() {
        /*
        with pageInfo3, with page info 4, WITH PAGE INFO5
         */
        val actionCluster = mutableMapOf<String, Action>()
        val json = GraphQLActionBuilderTest::class.java.getResource("/graphql/anilist(Fragment1PageInfo3Users2).json")
            .readText()

        val config = EMConfig()
        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster, config.treeDepth)

        assertEquals(1, actionCluster.size)
        val page = actionCluster.get("page") as GraphQLAction
        assertEquals(2, page.parameters.size)
        assertTrue(page.parameters[0] is GQInputParam)
        assertTrue((page.parameters[0].gene as OptionalGene).gene is IntegerGene)
        assertTrue(page.parameters[1] is GQReturnParam)

        assertTrue((page.parameters[1].gene as OptionalGene).gene is ObjectGene)
        val objPage = (page.parameters[1].gene as OptionalGene).gene as ObjectGene

        assertEquals(8, objPage.fields.size)
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "pageInfo" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "users" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "pageInfo2" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "pageInfo3" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "users2" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "pageInfo4" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "pageInfo5" })
        assertTrue(objPage.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "users3" })

        val tuplePageInfo = (objPage.fields.first { it.name == "pageInfo" } as OptionalGene).gene as TupleGene
        assertEquals(1, tuplePageInfo.elements.size)
        assertTrue(tuplePageInfo.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo" })

        val objPageInfo = (tuplePageInfo.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo.fields.size)
        assertTrue(objPageInfo.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "total" })
        val tupleTotal = (objPageInfo.fields.first { it.name == "total" } as OptionalGene).gene as TupleGene
        assertEquals(1, tupleTotal.elements.size)
        assertTrue(tupleTotal.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "total" })

        val tuplePageInfo2 = (objPage.fields.first { it.name == "pageInfo2" } as OptionalGene).gene as TupleGene
        assertEquals(1, tuplePageInfo2.elements.size)
        assertTrue(tuplePageInfo2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo2" })

        val objPageInfo2 = (tuplePageInfo2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo2.fields.size)
        assertTrue(objPageInfo2.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "total2" })
        val tupleTotal2 = (objPageInfo2.fields.first { it.name == "total2" } as OptionalGene).gene as TupleGene
        assertEquals(2, tupleTotal2.elements.size)
        assertTrue(tupleTotal2.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "id" })
        assertTrue(tupleTotal2.elements.any { it is OptionalGene && it.gene is BooleanGene && it.name == "total2" })

        val tuplePageInfo3 = (objPage.fields.first { it.name == "pageInfo3" } as OptionalGene).gene as TupleGene
        assertEquals(1, tuplePageInfo3.elements.size)
        assertTrue(tuplePageInfo3.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo3" })

        val objPageInfo3 = (tuplePageInfo3.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo3.fields.size)
        assertTrue(objPageInfo3.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "total3" })
        val tupleTotal3 = (objPageInfo3.fields.first { it.name == "total3" } as OptionalGene).gene as TupleGene
        assertEquals(1, tupleTotal3.elements.size)
        assertTrue(tupleTotal3.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "total3" })

        val objTotal3 = (tupleTotal3.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objTotal3.fields.size)
        assertTrue(objTotal3.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "price" })

        val tuplePrice = (objTotal3.fields.first { it.name == "price" } as OptionalGene).gene as TupleGene
        assertEquals(2, tuplePrice.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tuplePrice.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        assertTrue(tuplePrice.elements.any { it is OptionalGene && it.gene is IntegerGene && it.name == "price" })


        /*

         */
        val tupleUsers2 = (objPage.fields.first { it.name == "users2" } as OptionalGene).gene as TupleGene
        assertEquals(2, tupleUsers2.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search2" })
        assertTrue(tupleUsers2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "users2" })

        val objUser2 = (tupleUsers2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objUser2.fields.size)
        assertTrue(objUser2.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "about2" })

        val tupleAbout2 = (objUser2.fields.first { it.name == "about2" } as OptionalGene).gene as TupleGene
        assertEquals(1, tupleAbout2.elements.size)
        assertTrue(tupleAbout2.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "about2" })

        /*

         */
        val objAbout2 = (tupleAbout2.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objAbout2.fields.size)
        assertTrue(objAbout2.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "html" })

        val tupleHtml = (objAbout2.fields.first { it.name == "html" } as OptionalGene).gene as TupleGene
        assertEquals(2, tupleHtml.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tupleHtml.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Name" })
        assertTrue(tupleHtml.elements.any { it is OptionalGene && it.gene is BooleanGene && it.name == "html" })

        /*

         */
        val tuplePageInfo4 = (objPage.fields.first { it.name == "pageInfo4" } as OptionalGene).gene as TupleGene
        assertEquals(1, tuplePageInfo4.elements.size)
        assertTrue(tuplePageInfo4.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "pageInfo4" })

        /*

         */

        val tuplePageInfo5 = (objPage.fields.first { it.name == "pageInfo5" } as OptionalGene).gene as TupleGene
        assertEquals(1, tuplePageInfo5.elements.size)
        assertTrue(tuplePageInfo5.elements.any { it is OptionalGene && it.gene is ObjectGene && it.name == "pageInfo5" })

        val objPageInfo5 = (tuplePageInfo5.elements.last() as OptionalGene).gene as ObjectGene
        assertEquals(1, objPageInfo5.fields.size)
        assertTrue(objPageInfo5.fields.any { it is OptionalGene && it.gene is TupleGene && it.name == "total4" })

        val tupleTotal4 = (objPageInfo5.fields.first { it.name == "total4" } as OptionalGene).gene as TupleGene
        assertEquals(1, tupleTotal4.elements.size)
        assertTrue(tupleTotal4.elements.any { it is OptionalGene && it.gene is EnumGene<*> && it.name == "total4" })

        val enumTotal4 = (tupleTotal4.elements.last() as OptionalGene).gene as EnumGene<*>
        assertEquals(2, enumTotal4.values.size)
        assertTrue(enumTotal4.values.any { it == "TOTALENUM1" })
        assertTrue(enumTotal4.values.any { it == "TOTALENUM2" })

        /*

        */
        val tupleUsers3 = (objPage.fields.first { it.name == "users3" } as OptionalGene).gene as TupleGene
        assertEquals(3, tupleUsers3.elements.size)
        //This name is correct since it belongs to the input
        assertTrue(tupleUsers3.elements.any { it is OptionalGene && it.gene is StringGene && it.name == "Search" })
        assertTrue(tupleUsers3.elements.any { it is ArrayGene<*> && it.template is ObjectGene && it.name == "store" })
        val objStore = (tupleUsers3.elements.first { it.name == "store" } as ArrayGene<*>).template as ObjectGene
        assertEquals(1, objStore.fields.size)
        assertTrue(objStore.fields.any { it is IntegerGene && it.name == "id" })

    }


}