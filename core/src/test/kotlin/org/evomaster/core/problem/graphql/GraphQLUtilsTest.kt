package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File


class GraphQLUtilsTest {

    @Test
    fun graphTest() {

        val json = PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo2.json").readText()

        val state = GraphQLActionBuilder.TempState()
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        GraphQLActionBuilder.initTablesInfo(schemaObj, state)
        val queryGraph: MutableMap<String, MutableSet<String>> = mutableMapOf()
        GraphQLUtils.constructGraph(state, "query", " ", queryGraph,mutableListOf())
        Assertions.assertEquals(5,GraphQLUtils.getGraphSize(queryGraph))
        Assertions.assertEquals(6, GraphQLUtils.getNbrOfEdges(queryGraph))

    }


    @Test
    fun getStatsFromSchemas() {

        var jsonFiles: MutableMap<String, String> = mutableMapOf()
        var buffer = StringBuffer()

        jsonFiles.put("QueryTypeGlobalPetsClinic", PetClinicCheckMain::class.java.getResource("/graphql/QueryTypeGlobalPetsClinic.json").readText())
        jsonFiles.put("AniList", PetClinicCheckMain::class.java.getResource("/graphql/AniList.json").readText())
        jsonFiles.put("Bitquery", PetClinicCheckMain::class.java.getResource("/graphql/Bitquery.json").readText())
        jsonFiles.put("CatalysisHub", PetClinicCheckMain::class.java.getResource("/graphql/CatalysisHub.json").readText())
        jsonFiles.put("Contentful", PetClinicCheckMain::class.java.getResource("/graphql/Contentful.json").readText())
        jsonFiles.put("Countries", PetClinicCheckMain::class.java.getResource("/graphql/Countries.json").readText())
        jsonFiles.put("DeutscheBahn", PetClinicCheckMain::class.java.getResource("/graphql/DeutscheBahn.json").readText())
       // jsonFiles.put("DigitransitHSL", PetClinicCheckMain::class.java.getResource("/graphql/DigitransitHSL.json").readText())// did not get the stats for this one !!!

        jsonFiles.put("EHRI",PetClinicCheckMain::class.java.getResource("/graphql/EHRI.json").readText())
        jsonFiles.put("EtMDB",PetClinicCheckMain::class.java.getResource("/graphql/EtMDB.json").readText())
        jsonFiles.put("Everbase",PetClinicCheckMain::class.java.getResource("/graphql/Everbase.json").readText())
        jsonFiles.put("GitLab04022021",PetClinicCheckMain::class.java.getResource("/graphql/GitLab04022021.json").readText())
        jsonFiles.put("GraphQLJobs",PetClinicCheckMain::class.java.getResource("/graphql/GraphQLJobs.json").readText())
        jsonFiles.put("HIVDB",PetClinicCheckMain::class.java.getResource("/graphql/HIVDB.json").readText())
        jsonFiles.put("MelodyRepo",PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo.json").readText())
        jsonFiles.put("MelodyRepo2",PetClinicCheckMain::class.java.getResource("/graphql/MelodyRepo2.json").readText())
        jsonFiles.put("ReactFinland",PetClinicCheckMain::class.java.getResource("/graphql/ReactFinland.json").readText())
        jsonFiles.put("TravelgateX",PetClinicCheckMain::class.java.getResource("/graphql/TravelgateX.json").readText())
        jsonFiles.put("Universe",PetClinicCheckMain::class.java.getResource("/graphql/Universe.json").readText())
        jsonFiles.put("recEg",PetClinicCheckMain::class.java.getResource("/graphql/recEg.json").readText())
        jsonFiles.put("SpaceX",PetClinicCheckMain::class.java.getResource("/graphql/SpaceX.json").readText())
        jsonFiles.put("Book",PetClinicCheckMain::class.java.getResource("/graphql/Book.json").readText())
        jsonFiles.put("interfaceEg",PetClinicCheckMain::class.java.getResource("/graphql/interfaceEg.json").readText())//2?todo graph for interfaces
        jsonFiles.put("interfaceInternalEg",PetClinicCheckMain::class.java.getResource("/graphql/interfaceInternalEg.json").readText())//3?todo graph for interfaces
        jsonFiles.put("unionInternalEg",PetClinicCheckMain::class.java.getResource("/graphql/unionInternalEg.json").readText())//check th union with graph
        jsonFiles.put("unionInternalRecEg",PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg.json").readText())
        jsonFiles.put("unionInternalRecEg2",PetClinicCheckMain::class.java.getResource("/graphql/unionInternalRecEg2.json").readText())
        jsonFiles.put("enumInterface",PetClinicCheckMain::class.java.getResource("/graphql/enumInterface.json").readText())
        jsonFiles.put("interfaceHis",PetClinicCheckMain::class.java.getResource("/graphql/interfaceHis.json").readText())
        jsonFiles.put("noInterfaceHisObj",PetClinicCheckMain::class.java.getResource("/graphql/noInterfaceHisObj.json").readText())
        jsonFiles.put("recEg2",PetClinicCheckMain::class.java.getResource("/graphql/recEg2.json").readText())

        jsonFiles.forEach {
            val state = GraphQLActionBuilder.TempState()
            val gson = Gson()
            val schemaObj: SchemaObj = gson.fromJson(it.value, SchemaObj::class.java)
            GraphQLActionBuilder.initTablesInfo(schemaObj, state)

            val queryGraph: MutableMap<String, MutableSet<String>> = mutableMapOf()
            GraphQLUtils.constructGraph(state, "query", " ", queryGraph, mutableListOf())
            buffer.append("************* ${it.key}:").append(System.getProperty("line.separator"))

            if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                buffer.append("Number Of Node In The Query : ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))//19
                buffer.append("Number Of Edges In The Query : ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
            } else {
                GraphQLUtils.constructGraph(state, "querytype", " ", queryGraph, mutableListOf())
                if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                    buffer.append("Number Of Node In The Query : ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))//19
                    buffer.append("Number Of Edges In The Query : ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                }
                else {
                    GraphQLUtils.constructGraph(state, "root", " ", queryGraph, mutableListOf())
                    if (GraphQLUtils.getGraphSize(queryGraph) != 0) {
                        buffer.append("Number Of Node In The Query : ${GraphQLUtils.getGraphSize(queryGraph)}").append(System.getProperty("line.separator"))//19
                        buffer.append("Number Of Edges In The Query : ${GraphQLUtils.getNbrOfEdges(queryGraph)}").append(System.getProperty("line.separator"))
                    }
                }
            }

            val mutationGraph: MutableMap<String, MutableSet<String>> = mutableMapOf()
            GraphQLUtils.constructGraph(state, "mutation", " ", mutationGraph, mutableListOf())
            buffer.append("Number Of Node In The Mutation : ${GraphQLUtils.getGraphSize(mutationGraph)}").append(System.getProperty("line.separator"))
            buffer.append("Number Of Edges In The Mutation : ${GraphQLUtils.getNbrOfEdges(mutationGraph)}").append(System.getProperty("line.separator"))

        }
        File("src/test/resources/graphql/stats").bufferedWriter().use { out -> out.append(buffer) }

    }
}