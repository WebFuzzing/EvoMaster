package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*


class GraphQLUtilsTest {

    val unionTag = "#UNION#"
    val interfaceTag = "#INTERFACE#"

    @Test
    fun graphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo2.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(4, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(2, GraphQLUtils.getNbrQueriesOrMutations("query", queryGraph))
        Assertions.assertEquals(3, GraphQLUtils.getNbrOfEdges(queryGraph))
        Assertions.assertEquals(listOf(2, 1, 1), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 3, path = listOf("query", "Package", "Version")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(0, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))
    }

    @Test
    fun petClinicFragmentGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/PetsClinic(Fragment).json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(5, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(7, 5, 0, 0), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 4, path = listOf("query", "Owner", "Pet", "VisitConnection")), GraphQLUtils.longest(paths))

    }


    @Test
    fun petClinicFragment2GraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/PetsClinic(Fragment2).json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(5, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(7, 6, 0, 0), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 4, path = listOf("query", "Owner", "Pet", "VisitConnection")), GraphQLUtils.longest(paths))

    }


    @Test
    fun cyclesEgGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/CyclesEg.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(3, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(4, 2), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 3, path = listOf("query", "Job", "Commitment")), GraphQLUtils.longest(paths))

    }


    @Test
    fun universeCyclesFragmentGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/UniverseCycles(Fragment).json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val mutationGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "mutation", " ", mutationGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(4, GraphQLUtils.getGraphSize(mutationGraph))
        Assertions.assertEquals(1, GraphQLUtils.getNbrQueriesOrMutations("mutation", mutationGraph))
        Assertions.assertEquals(listOf(1, 1, 1), GraphQLUtils.getNbrFields(mutationGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "mutation", mutationGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 4, path = listOf("mutation", "AddOnCreatePayload", "AddOn", "AddOnRate")), GraphQLUtils.longest(paths))

    }


    @Test
    fun unionInternalEgFragmentGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/unionInternalEg(Fragment).json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(5, GraphQLUtils.getGraphSize(queryGraph))
        println(GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf()))
        Assertions.assertEquals(listOf(1, 1, 1, 1), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 4, path = listOf("query", "Store", "Bouquet", "Pot")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))

    }

    @Test
    fun unionInternalRecEgFragmentGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg(Fragment).json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(5, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(1, 1, 2, 1), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 4, path = listOf("query", "Store", "Bouquet", "Pot")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))
    }

    @Test
    fun unionInternalRecEg2GraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg2.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(6, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(1, 1, 1, 2, 1), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 5, path = listOf("query", "VulnerabilityConnection", "Vulnerability", "VulnerabilityDetail", "VulnerabilityDetailTable")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))
    }

    @Test
    fun interfaceInternalEgGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/interfaceInternalEg.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(5, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(1, 3, 2, 3), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 4, path = listOf("query", "Store1", "Store", "PotStore")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph))
    }


    @Test
    fun interfaceEgGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/interfaceEg.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(4, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(3, 2, 3), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 3, path = listOf("query", "Store", "PotStore")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph))
    }


    @Test
    fun interfaceHisGraphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/interfaceHis.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(16, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(2, 1, 1, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(GraphQLUtils.LongestInfo(size = 6, path = listOf("query", "Node", "Agency", "Route", "Pattern", "Trip")), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph))
    }

    @Test
    fun getStatsFromSchemas() {

        val jsonFiles: MutableMap<String, String> = mutableMapOf()
        val buffer = StringBuffer()

        jsonFiles.put("QueryTypeGlobalPetsClinic", PetClinicCheckMain::class.java.getResource("/graphql/PetsClinic.json").readText())
        jsonFiles.put("AniList", PetClinicCheckMain::class.java.getResource("/graphql/AniList.json").readText())
        jsonFiles.put("Bitquery", PetClinicCheckMain::class.java.getResource("/graphql/Bitquery.json").readText())
        jsonFiles.put("GitLab04022021", PetClinicCheckMain::class.java.getResource("/graphql/GitLab.json").readText())
        jsonFiles.put("DigitransitHSL", PetClinicCheckMain::class.java.getResource("/graphql/DigitransitHSL.json").readText())
        jsonFiles.put("TravelgateX", PetClinicCheckMain::class.java.getResource("/graphql/TravelgateX.json").readText())
        jsonFiles.put("Universe", PetClinicCheckMain::class.java.getResource("/graphql/Universe.json").readText())
        jsonFiles.put("CatalysisHub", PetClinicCheckMain::class.java.getResource("/graphql/CatalysisHub.json").readText())
        jsonFiles.put("Contentful", PetClinicCheckMain::class.java.getResource("/graphql/Contentful.json").readText())
        jsonFiles.put("Countries", PetClinicCheckMain::class.java.getResource("/graphql/Countries.json").readText())
        jsonFiles.put("DeutscheBahn", PetClinicCheckMain::class.java.getResource("/graphql/DeutscheBahn.json").readText())
        jsonFiles.put("EHRI", PetClinicCheckMain::class.java.getResource("/graphql/EHRI.json").readText())
        jsonFiles.put("EtMDB", PetClinicCheckMain::class.java.getResource("/graphql/EtMDB.json").readText())
        jsonFiles.put("Everbase", PetClinicCheckMain::class.java.getResource("/graphql/Everbase.json").readText())
        jsonFiles.put("GraphQLJobs", PetClinicCheckMain::class.java.getResource("/graphql/GraphQLJobs.json").readText())
        jsonFiles.put("HIVDB", PetClinicCheckMain::class.java.getResource("/graphql/HIVDB.json").readText())
        jsonFiles.put("MelodyRepo", PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo.json").readText())
        jsonFiles.put("MelodyRepo2", PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo2.json").readText())
        jsonFiles.put("ReactFinland", PetClinicCheckMain::class.java.getResource("/graphql/ReactFinland.json").readText())
        jsonFiles.put("recEg", PetClinicCheckMain::class.java.getResource("/graphql/recEg.json").readText())
        jsonFiles.put("SpaceX", PetClinicCheckMain::class.java.getResource("/graphql/SpaceX.json").readText())
        jsonFiles.put("Book", PetClinicCheckMain::class.java.getResource("/graphql/Book.json").readText())
        jsonFiles.put("interfaceEg", PetClinicCheckMain::class.java.getResource("/graphql/interfaceEg.json").readText())//2?todo graph for interfaces
        jsonFiles.put("interfaceInternalEg", PetClinicCheckMain::class.java.getResource("/graphql/interfaceInternalEg.json").readText())//3?todo graph for interfaces
        jsonFiles.put("unionInternalEg", PetClinicCheckMain::class.java.getResource("/graphql/unionInternalEg.json").readText())//todo check th union with graph
        jsonFiles.put("unionInternalRecEg", PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg.json").readText())
        jsonFiles.put("unionInternalRecEg2", PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg2.json").readText())
        jsonFiles.put("enumInterface", PetClinicCheckMain::class.java.getResource("/graphql/enumInterface.json").readText())
        jsonFiles.put("interfaceHis", PetClinicCheckMain::class.java.getResource("/graphql/interfaceHis.json").readText())
        jsonFiles.put("noInterfaceHisObj", PetClinicCheckMain::class.java.getResource("/graphql/noInterfaceHisObj.json").readText())
        jsonFiles.put("recEg2", PetClinicCheckMain::class.java.getResource("/graphql/recEg2.json").readText())

        jsonFiles.forEach {
            val state = GraphQLActionBuilder.TempState()
            val gson = Gson()
            val schemaObj: SchemaObj = gson.fromJson(it.value, SchemaObj::class.java)
            GraphQLActionBuilder.initTablesInfo(schemaObj, state)

            var visitedVertex: MutableSet<String> = mutableSetOf()
            var stack: Deque<String> = ArrayDeque<String>()
            var paths: MutableList<List<String>> = mutableListOf()
            val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()

            GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
            buffer.append("************* ${it.key}:").append(System.getProperty("line.separator"))


            if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                buffer.append("The number of fields in the type Query: ${GraphQLUtils.getNbrQueriesOrMutations("query", queryGraph)}").append(System.getProperty("line.separator"))
                buffer.append("The number of fields in all nodes, excluding Query: ${GraphQLUtils.getNbrFields(queryGraph)}").append(System.getProperty("line.separator"))
                buffer.append("The number of edge (number of fields that are pointers to other nodes in the graph) including Query: ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                buffer.append("The number of nodes, including Query: ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))
                GraphQLUtils.getAllPaths(visitedVertex, stack, "query", queryGraph, paths)
                buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph)}").append(System.getProperty("line.separator"))
                buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph)}").append(System.getProperty("line.separator"))

            } else {
                GraphQLUtils.constructGraph(state, "querytype", " ", queryGraph, mutableListOf(), mutableSetOf())
                if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                    buffer.append("The number of fields in the type Query: ${GraphQLUtils.getNbrQueriesOrMutations("querytype", queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of fields in all nodes, excluding Query: ${GraphQLUtils.getNbrFields(queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of edge (number of fields that are pointers to other nodes in the graph) including Query: ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of nodes, including Query: ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))
                    GraphQLUtils.getAllPaths(visitedVertex, stack, "querytype", queryGraph, paths)
                    buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph)}").append(System.getProperty("line.separator"))

                } else {
                    GraphQLUtils.constructGraph(state, "root", " ", queryGraph, mutableListOf(), mutableSetOf())
                    if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                        buffer.append("The number of fields in the type Query: ${GraphQLUtils.getNbrQueriesOrMutations("root", queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of fields in all nodes, excluding Query: ${GraphQLUtils.getNbrFields(queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of edge (number of fields that are pointers to other nodes in the graph) including Query: ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of nodes, including Query: ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))
                        GraphQLUtils.getAllPaths(visitedVertex, stack, "root", queryGraph, paths)
                        buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph)}").append(System.getProperty("line.separator"))

                    }
                }
            }

            val mutationGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
            visitedVertex = mutableSetOf()
            stack = ArrayDeque<String>()
            paths = mutableListOf()

            GraphQLUtils.constructGraph(state, "mutation", " ", mutationGraph, mutableListOf(), mutableSetOf())
            buffer.append("--**--").append(System.getProperty("line.separator"))
            buffer.append("The number of fields in the type Mutation: ${GraphQLUtils.getNbrQueriesOrMutations("mutation", mutationGraph)}").append(System.getProperty("line.separator"))
            buffer.append("The number of fields in all nodes, excluding Mutation: ${GraphQLUtils.getNbrFields(mutationGraph)}").append(System.getProperty("line.separator"))
            buffer.append("The number of edge (number of fields that are pointers to other nodes in the graph) including Mutation: ${GraphQLUtils.getNbrOfEdges(mutationGraph)}").append(System.getProperty("line.separator"))
            buffer.append("The number of nodes, including Mutation: ${GraphQLUtils.getGraphSize(mutationGraph)}").append(System.getProperty("line.separator"))
            buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, mutationGraph)}").append(System.getProperty("line.separator"))
            buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, mutationGraph)}").append(System.getProperty("line.separator"))

            if (GraphQLUtils.checkNode("mutation", mutationGraph)) {
                GraphQLUtils.getAllPaths(visitedVertex, stack, "mutation", mutationGraph, paths)
                buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
            }

        }
        File("src/test/resources/graphql/stats").bufferedWriter().use { out -> out.append(buffer) }

    }
}