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

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/MelodyRepo2.json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Package", "Version"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(0, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))
    }

    @Test
    fun petClinicFragmentGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/PetsClinic(Fragment).json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Owner", "Pet", "PetType"), GraphQLUtils.longest(paths))
    }


    @Test
    fun petClinicFragment2GraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/PetsClinic(Fragment2).json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Owner", "Pet", "PetType"), GraphQLUtils.longest(paths))

    }


    @Test
    fun cyclesEgGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/CyclesEg.json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Job", "Commitment"), GraphQLUtils.longest(paths))

    }


    @Test
    fun universeCyclesFragmentGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/UniverseCycles(Fragment).json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "mutation", mutationGraph, paths)
        Assertions.assertEquals(listOf("mutation", "AddOnCreatePayload", "AddOn", "AddOnRate"), GraphQLUtils.longest(paths))

    }


    @Test
    fun unionInternalEgFragmentGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/unionInternalEg(Fragment).json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(5, GraphQLUtils.getGraphSize(queryGraph))
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(listOf(1, 1, 1, 1), GraphQLUtils.getNbrFields(queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Store", "Bouquet", "Flower"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))

    }

    @Test
    fun unionInternalRecEgFragmentGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/unionInternalRecEg(Fragment).json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Store", "Bouquet", "Flower"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))
    }

    @Test
    fun unionInternalRecEg2GraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/unionInternalRecEg2.json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "VulnerabilityConnection", "Vulnerability", "VulnerabilityDetail", "VulnerabilityDetailList"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph))
    }

    @Test
    fun interfaceInternalEgGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/interfaceInternalEg.json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Store1", "Store", "FlowerStore"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph))
    }


    @Test
    fun interfaceEgGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/interfaceEg.json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Store", "FlowerStore"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph))
    }


    @Test
    fun interfaceHisGraphTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/interfaceHis.json").readText()

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
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
        Assertions.assertEquals(listOf("query", "Node", "Agency", "Route", "Pattern", "Trip"), GraphQLUtils.longest(paths))
        Assertions.assertEquals(1, GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph))
    }


    @Test
    fun allNodesReachableTest() {

        val json = GraphQLUtilsTest::class.java.getResource("/graphql/abstract.json").readText()
        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, GraphQLUtils.GraphInfo> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf(), mutableSetOf())
        Assertions.assertEquals(7, GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(listOf(3, 1, 1, 2, 1, 1), GraphQLUtils.getNbrFields(queryGraph))
        Assertions.assertEquals(setOf("E"), GraphQLUtils.getAdjacent("D", queryGraph))
        /**/
        val visitedVertex: MutableSet<String> = mutableSetOf()
        val stack: Deque<String> = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)

        /**/
        Assertions.assertEquals(listOf(listOf("A", "B", "C", "D", "E"), listOf("A", "F", "E")), GraphQLUtils.getAllPaths("A", "E", queryGraph))
        Assertions.assertEquals(listOf(listOf("query", "A", "B", "C", "D", "E"), listOf("query", "A", "F", "E"), listOf("query", "B", "C", "D", "E")), GraphQLUtils.getAllPaths("query", "E", queryGraph))
        /**/
        Assertions.assertEquals(listOf("query", "A", "F", "E"), GraphQLUtils.shortestPath(GraphQLUtils.getAllPaths("query", "E", queryGraph)))
        /**/
        val shortestPFromEachEP = GraphQLUtils.getShortestPathFromEachEntryPointToEachNode(queryGraph)

        Assertions.assertEquals(listOf(listOf("A", "B"),
                listOf("A", "B", "C"),
                listOf("A", "B", "C", "D"),
                listOf("A", "F", "E"),
                listOf("A", "F"),
                listOf(),
                listOf("B", "C"),
                listOf("B", "C", "D"),
                listOf("B", "C", "D", "E"),
                listOf()), shortestPFromEachEP)

        val minPAmongAllEPForEachNode = GraphQLUtils.minPathAmongAllEntryPointsForEachNode(shortestPFromEachEP)

        Assertions.assertEquals(listOf(listOf("B", "C"),
                listOf("B", "C", "D"),
                listOf("A", "F"),
                listOf("A", "F", "E")), minPAmongAllEPForEachNode)

        val maxPathAmongAllEPForAllNodes = GraphQLUtils.maxPathAmongAllEntryPointsForAllNodes(minPAmongAllEPForEachNode)

        Assertions.assertEquals(GraphQLUtils.PathInfo(size = 3, path = listOf("B", "C", "D")), maxPathAmongAllEPForAllNodes)
    }


    @Test
    fun getStatsFromSchemas() {

        val jsonFiles: MutableMap<String, String> = mutableMapOf()
        val buffer = StringBuffer()

        jsonFiles["PetsClinic"] = GraphQLUtilsTest::class.java.getResource("/graphql/PetsClinic.json").readText()
        jsonFiles["AniList"] = GraphQLUtilsTest::class.java.getResource("/graphql/AniList.json").readText()
        jsonFiles["Bitquery"] = GraphQLUtilsTest::class.java.getResource("/graphql/Bitquery.json").readText()
        // jsonFiles["GitLab"] = GraphQLUtilsTest::class.java.getResource("/graphql/GitLab.json").readText()//Todo not working
        jsonFiles["DigitransitHSL"] = GraphQLUtilsTest::class.java.getResource("/graphql/DigitransitHSL.json").readText()
        jsonFiles["TravelgateX"] = GraphQLUtilsTest::class.java.getResource("/graphql/TravelgateX.json").readText()
        jsonFiles["Universe"] = GraphQLUtilsTest::class.java.getResource("/graphql/Universe.json").readText()
        jsonFiles["CatalysisHub"] = GraphQLUtilsTest::class.java.getResource("/graphql/CatalysisHub.json").readText()
        jsonFiles["Contentful"] = GraphQLUtilsTest::class.java.getResource("/graphql/Contentful.json").readText()
        jsonFiles["Countries"] = GraphQLUtilsTest::class.java.getResource("/graphql/Countries.json").readText()
        jsonFiles["DeutscheBahn"] = GraphQLUtilsTest::class.java.getResource("/graphql/DeutscheBahn.json").readText()
        jsonFiles["EHRI"] = GraphQLUtilsTest::class.java.getResource("/graphql/EHRI.json").readText()
        jsonFiles["EtMDB"] = GraphQLUtilsTest::class.java.getResource("/graphql/EtMDB.json").readText()
        jsonFiles["Everbase"] = GraphQLUtilsTest::class.java.getResource("/graphql/Everbase.json").readText()
        jsonFiles["GraphQLJobs"] = GraphQLUtilsTest::class.java.getResource("/graphql/GraphQLJobs.json").readText()
        jsonFiles["HIVDB"] = GraphQLUtilsTest::class.java.getResource("/graphql/HIVDB.json").readText()
        jsonFiles["MelodyRepo"] = GraphQLUtilsTest::class.java.getResource("/graphql/MelodyRepo.json").readText()
        jsonFiles["MelodyRepo2"] = GraphQLUtilsTest::class.java.getResource("/graphql/MelodyRepo2.json").readText()
        jsonFiles["ReactFinland"] = GraphQLUtilsTest::class.java.getResource("/graphql/ReactFinland.json").readText()
        jsonFiles["recEg"] = GraphQLUtilsTest::class.java.getResource("/graphql/recEg.json").readText()
        jsonFiles["SpaceX"] = GraphQLUtilsTest::class.java.getResource("/graphql/SpaceX.json").readText()
        jsonFiles["Book"] = GraphQLUtilsTest::class.java.getResource("/graphql/Book.json").readText()
        jsonFiles["interfaceEg"] = GraphQLUtilsTest::class.java.getResource("/graphql/interfaceEg.json").readText()
        jsonFiles["interfaceInternalEg"] = GraphQLUtilsTest::class.java.getResource("/graphql/interfaceInternalEg.json").readText()
        jsonFiles["unionInternalEg"] = GraphQLUtilsTest::class.java.getResource("/graphql/unionInternalEg.json").readText()
        jsonFiles["unionInternalRecEg"] = GraphQLUtilsTest::class.java.getResource("/graphql/unionInternalRecEg.json").readText()
        jsonFiles["unionInternalRecEg2"] = GraphQLUtilsTest::class.java.getResource("/graphql/unionInternalRecEg2.json").readText()
        jsonFiles["enumInterface"] = GraphQLUtilsTest::class.java.getResource("/graphql/enumInterface.json").readText()
        jsonFiles["interfaceHis"] = GraphQLUtilsTest::class.java.getResource("/graphql/interfaceHis.json").readText()
        jsonFiles["recEg2"] = GraphQLUtilsTest::class.java.getResource("/graphql/recEg2.json").readText()

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
                GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "query", queryGraph, paths)
                buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph)}").append(System.getProperty("line.separator"))
                buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph)}").append(System.getProperty("line.separator"))
                buffer.append("The maximum path of the minimum among all entry points: ${GraphQLUtils.maxPathAmongAllEntryPointsForAllNodes(GraphQLUtils.minPathAmongAllEntryPointsForEachNode(GraphQLUtils.getShortestPathFromEachEntryPointToEachNode(queryGraph)))}").append(System.getProperty("line.separator"))
            } else {
                GraphQLUtils.constructGraph(state, "querytype", " ", queryGraph, mutableListOf(), mutableSetOf())
                if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                    buffer.append("The number of fields in the type Query: ${GraphQLUtils.getNbrQueriesOrMutations("querytype", queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of fields in all nodes, excluding Query: ${GraphQLUtils.getNbrFields(queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of edge (number of fields that are pointers to other nodes in the graph) including Query: ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of nodes, including Query: ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))
                    GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "querytype", queryGraph, paths)
                    buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph)}").append(System.getProperty("line.separator"))
                    buffer.append("The maximum path of the minimum among all entry points: ${GraphQLUtils.maxPathAmongAllEntryPointsForAllNodes(GraphQLUtils.minPathAmongAllEntryPointsForEachNode(GraphQLUtils.getShortestPathFromEachEntryPointToEachNode(queryGraph)))}").append(System.getProperty("line.separator"))

                } else {
                    GraphQLUtils.constructGraph(state, "root", " ", queryGraph, mutableListOf(), mutableSetOf())
                    if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                        buffer.append("The number of fields in the type Query: ${GraphQLUtils.getNbrQueriesOrMutations("root", queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of fields in all nodes, excluding Query: ${GraphQLUtils.getNbrFields(queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of edge (number of fields that are pointers to other nodes in the graph) including Query: ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of nodes, including Query: ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))
                        GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "root", queryGraph, paths)
                        buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of unions: ${GraphQLUtils.getUnionOrInterfaceNbr(unionTag, queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The number of interfaces: ${GraphQLUtils.getUnionOrInterfaceNbr(interfaceTag, queryGraph)}").append(System.getProperty("line.separator"))
                        buffer.append("The maximum path of the minimum among all entry points: ${GraphQLUtils.maxPathAmongAllEntryPointsForAllNodes(GraphQLUtils.minPathAmongAllEntryPointsForEachNode(GraphQLUtils.getShortestPathFromEachEntryPointToEachNode(queryGraph)))}").append(System.getProperty("line.separator"))

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
                GraphQLUtils.getAllPathsFromEntryPoint(visitedVertex, stack, "mutation", mutationGraph, paths)
                buffer.append("The longest path is: ${GraphQLUtils.longest(paths)}").append(System.getProperty("line.separator"))
                buffer.append("The maximum path of the minimum among all entry points: ${GraphQLUtils.maxPathAmongAllEntryPointsForAllNodes(GraphQLUtils.minPathAmongAllEntryPointsForEachNode(GraphQLUtils.getShortestPathFromEachEntryPointToEachNode(mutationGraph)))}").append(System.getProperty("line.separator"))

            }

        }
        File("src/test/resources/graphql/stats").bufferedWriter().use { out -> out.append(buffer) }

    }
}