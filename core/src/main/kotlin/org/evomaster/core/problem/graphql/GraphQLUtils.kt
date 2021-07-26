package org.evomaster.core.problem.graphql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.*
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.client.Entity

object GraphQLUtils {

    private val log = LoggerFactory.getLogger(GraphQLUtils::class.java)

    const val unionTag = "#UNION#"
    const val interfaceTag = "#INTERFACE#"

    val history: Deque<String> = ArrayDeque<String>()

    fun generateGQLBodyEntity(a: GraphQLAction, targetFormat: OutputFormat): Entity<String>? {

        //TOdo check empty return type
        val returnGene = a.parameters.find { p -> p is GQReturnParam }?.gene

        val inputGenes = a.parameters.filterIsInstance<GQInputParam>().map { it.gene }

        var bodyEntity: Entity<String> = Entity.json(" ")


        if (a.methodType == GQMethodType.QUERY) {

            if (inputGenes.isNotEmpty()) {

                val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes, targetFormat)

                val printableInputGenes = getPrintableInputGenes(printableInputGene)

                //primitive type in Return
                bodyEntity = if (returnGene == null) {
                    Entity.json("""
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)         } ","variables":null}
                """.trimIndent())

                } else if (returnGene.name.endsWith(ObjectGene.unionTag)) {//The first is a union type

                    var query = getQuery(returnGene, a)//todo remove the name for the first union
                    Entity.json("""
                   {"query" : " {  ${a.methodName} ($printableInputGenes)  { $query }  }   ","variables":null}
                """.trimIndent())

                } else {
                    val query = getQuery(returnGene, a)
                    Entity.json("""
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)  $query       } ","variables":null}
                """.trimIndent())

                }
            } else {//request without arguments
                bodyEntity = if (returnGene == null) { //primitive type
                    Entity.json("""
                    {"query" : "  { ${a.methodName}       } ","variables":null}
                """.trimIndent())

                }
                /*  else if (returnGene.name.endsWith(ObjectGene.unionTag)|| returnGene.name.endsWith(ObjectGene.interfaceTag)) {//The first one is a union

                      var query = getQuery(returnGene, a)
                      Entity.json("""
                     {"query" : "    { $query }     ","variables":null}
                  """.trimIndent())

                  } */
                else {
                    var query = getQuery(returnGene, a)
                    Entity.json("""
                   {"query" : " {  ${a.methodName}  $query   }   ","variables":null}
                """.trimIndent())
                }
            }
        } else if (a.methodType == GQMethodType.MUTATION) {
            val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes, targetFormat)

            val printableInputGenes = getPrintableInputGenes(printableInputGene)

            /*
                Need a check with Asma
                for mutation which does not have any param, there is no need for ()
                e.g., createX:X!
                      mutation{
                        createX{
                            ...
                        }
                      }
             */
            val inputParams = if (printableInputGene.isEmpty()) "" else "($printableInputGenes)"
            bodyEntity = if (returnGene == null) {//primitive type
                Entity.json("""
                {"query" : " mutation{ ${a.methodName}  $inputParams         } ","variables":null}
            """.trimIndent())

            } else {
                val mutation = getMutation(returnGene, a)
                Entity.json("""
                { "query" : "mutation{    ${a.methodName}  $inputParams    $mutation    }","variables":null}
            """.trimIndent())

            }
        } else {
            LoggingUtil.uniqueWarn(log, " method type not supported yet : ${a.methodType}")
            return null
        }
        return bodyEntity
    }

    fun getMutation(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }

    fun getQuery(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }


    fun getPrintableInputGenes(printableInputGene: MutableList<String>): String {
        // Man: is the fun to handle " in printable
        return printableInputGene.joinToString(",").replace("\"", "\\\"")

    }

    fun getPrintableInputGene(inputGenes: List<Gene>, targetFormat: OutputFormat? = null): MutableList<String> {
        val printableInputGene = mutableListOf<String>()
        for (gene in inputGenes) {
            if (gene is EnumGene<*> || (gene is OptionalGene && gene.gene is EnumGene<*>)) {
                val i = gene.getValueAsRawString()
                printableInputGene.add("${gene.name} : $i")
            } else {
                if (gene is ObjectGene || (gene is OptionalGene && gene.gene is ObjectGene)) {
                    val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
                    printableInputGene.add(" $i")
                } else {
                    if (gene is ArrayGene<*> || (gene is OptionalGene && gene.gene is ArrayGene<*>)) {
                        val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE)
                        printableInputGene.add("${gene.name} : $i")
                    } else {
                        /*
                            TODO
                            Man: this is a temporal solution
                            there might also need a further handling, e.g., field of object is String, Array<String>
                         */
                        val mode = if (ParamUtil.getValueGene(gene) is StringGene) GeneUtils.EscapeMode.GQL_STR_VALUE else GeneUtils.EscapeMode.GQL_INPUT_MODE
                        val i = gene.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
                        printableInputGene.add("${gene.name} : $i")
                    }
                }
            }
        }
        return printableInputGene
    }


    fun repairIndividual(ind: GraphQLIndividual) {
        ind.seeActions().forEach { a ->
            a.parameters.filterIsInstance<GQReturnParam>().forEach { p ->
                if (p.gene is ObjectGene) {
                    GeneUtils.repairBooleanSelection(p.gene)
                }
            }
        }
    }


    data class GraphInfo(
            var fields: MutableSet<String> = mutableSetOf(),
            var edges: MutableSet<String> = mutableSetOf()//set to omit redundant nodes

    )

    data class LongestInfo(
            var size: Int = 0,
            var path: List<String> = listOf()
    )

    fun constructGraph(state: GraphQLActionBuilder.TempState, entryPoint: String, source: String, graph: MutableMap<String, GraphInfo>, history: MutableList<String>, objectFieldsHistory: MutableSet<String>): MutableMap<String, GraphInfo> {

        for (element in state.tables) {
            if (element.tableType?.toLowerCase() != entryPoint.toLowerCase()) {
                continue
            }

            if (entryPoint.toLowerCase() == "query" || entryPoint.toLowerCase() == "querytype" || entryPoint.toLowerCase() == "root" || entryPoint.toLowerCase() == "mutation") {//First entry point

                if (element.kindOfTableFieldType.toString().toLowerCase() == "object") {
                    history.add(element.tableFieldType)
                    addEdge(element.tableType!!.toLowerCase(), element.tableFieldType, element.tableField, graph)
                    if (history.count { it == element.tableFieldType } <= 1) {
                        if (!objectFieldsHistory.contains(element.tableFieldType)) {
                            constructGraph(state, element.tableFieldType, element.tableField, graph, history, objectFieldsHistory)
                            history.remove(element.tableFieldType)
                            objectFieldsHistory.add(element.tableFieldType)
                        } else {
                            history.remove(element.tableFieldType)
                        }
                    } else {
                        history.remove(element.tableFieldType)
                    }
                } else {
                    if (element.kindOfTableFieldType.toString().toLowerCase() == "scalar" || element.kindOfTableFieldType.toString().toLowerCase() == "enum") {//Primitive type: create a field and add it
                        graph[element.tableType!!.toLowerCase()]?.fields?.add(element.tableField)
                    } else {
                        if (element.kindOfTableFieldType.toString().toLowerCase() == "interface") {
                            history.add(element.tableFieldType)// add interface type object to the history
                            addEdge(element.tableType!!.toLowerCase(), element.tableFieldType, element.tableField, graph)//from a query node to the interface type object
                            if (history.count { it == element.tableFieldType } <= 1) {// The interface type object is already treated
                                constructGraph(state, element.tableFieldType, "", graph, history, objectFieldsHistory)//Construct the base fields of the interface
                                element.interfaceTypes.forEach { ob ->
                                    history.add(ob)//add the interface objects to the history
                                    addEdge(element.tableFieldType, ob, interfaceTag, graph) // from the interface type object to its objects
                                    if (history.count { it == ob } <= 1) {
                                        constructGraph(state, ob, "", graph, history, objectFieldsHistory)
                                        history.remove(ob)
                                    } else {
                                        history.remove(ob)
                                    }
                                }
                                history.remove(element.tableFieldType)// remove the interface type object from the history: the normal case (not cycle)
                            } else {
                                history.remove(element.tableFieldType)// remove the interface type object to the history: the cycle case
                            }
                        } else {
                            if (element.kindOfTableFieldType.toString().toLowerCase() == "union") {
                                history.add(element.tableFieldType)// add union type object to the history
                                addEdge(element.tableType!!.toLowerCase(), element.tableFieldType, element.tableField, graph)//from a query node to the union type object
                                if (history.count { it == element.tableFieldType } <= 1) {// the union type object already treated
                                    element.unionTypes.forEach { ob ->
                                        history.add(ob)//add the union objects to the history
                                        addEdge(element.tableFieldType, ob, unionTag, graph) // from the union type object to its objects
                                        if (history.count { it == ob } <= 1) {
                                            constructGraph(state, ob, "", graph, history, objectFieldsHistory)
                                            history.remove(ob)
                                        } else {
                                            history.remove(ob)
                                        }
                                    }
                                    history.remove(element.tableFieldType)// remove the union type object to the history: the normal case (not cycle)
                                } else {
                                    history.remove(element.tableFieldType)// remove the union type object to the history: the cycle case
                                }
                            }
                        }
                    }
                }
            } else {//Second entry

                if (element.kindOfTableFieldType.toString().toLowerCase() == "object") {
                    history.add(element.tableFieldType)
                    addEdge(entryPoint, element.tableFieldType, element.tableField, graph)
                    if (history.count { it == element.tableFieldType } <= 1) {
                        if (!objectFieldsHistory.contains(element.tableFieldType)) {
                            constructGraph(state, element.tableFieldType, element.tableField, graph, history, objectFieldsHistory)
                            history.remove(element.tableFieldType)
                            objectFieldsHistory.add(element.tableFieldType)
                        } else {
                            history.remove(element.tableFieldType)
                        }
                    } else {
                        history.remove(element.tableFieldType)
                    }
                } else {
                    if (element.kindOfTableFieldType.toString().toLowerCase() == "scalar" || element.kindOfTableFieldType.toString().toLowerCase() == "enum") {//Primitive type: create a field and add it
                        graph[entryPoint]?.fields?.add(element.tableField)
                    } else {
                        if (element.kindOfTableFieldType.toString().toLowerCase() == "union") {
                            history.add(element.tableFieldType)// add union type object to the history
                            addEdge(entryPoint, element.tableFieldType, element.tableField, graph)//from a node to the union type object
                            if (history.count { it == element.tableFieldType } <= 1) {// the union type object is already treated
                                element.unionTypes.forEach { ob ->
                                    history.add(ob)//add the union objects to the history
                                    addEdge(element.tableFieldType, ob, unionTag, graph) // from the union type object to its objects
                                    if (history.count { it == ob } <= 1) {
                                        constructGraph(state, ob, "", graph, history, objectFieldsHistory)
                                        history.remove(ob)
                                    } else {
                                        history.remove(ob)
                                    }
                                }
                                history.remove(element.tableFieldType)// remove the union type object to the history: the normal case (not cycle)
                            } else {
                                history.remove(element.tableFieldType)// remove the union type object to the history:the cycle case
                            }
                        } else {
                            if (element.kindOfTableFieldType.toString().toLowerCase() == "interface") {
                                history.add(element.tableFieldType)// add interface type object to the history
                                addEdge(entryPoint, element.tableFieldType, element.tableField, graph)//from a node to the interface type object
                                if (history.count { it == element.tableFieldType } <= 1) {// the interface type object already treated
                                    constructGraph(state, element.tableFieldType, "", graph, history, objectFieldsHistory)//construct the base fields of the interface
                                    element.interfaceTypes.forEach { ob ->
                                        history.add(ob)//add the interface objects to the history
                                        addEdge(element.tableFieldType, ob, interfaceTag, graph) // from the interface type object to its objects
                                        if (history.count { it == ob } <= 1) {
                                            constructGraph(state, ob, "", graph, history, objectFieldsHistory)
                                            history.remove(ob)
                                        } else {
                                            history.remove(ob)
                                        }
                                    }
                                    history.remove(element.tableFieldType)// remove the interface type object to the history:the normal case (not cycle)
                                } else {
                                    history.remove(element.tableFieldType)// remove the interface type object to the history:the cycle case
                                }
                            }
                        }
                    }

                }
            }
        }
        return graph
    }

    private fun addNode(node: String, graph: MutableMap<String, GraphInfo>): GraphInfo? {
        return graph.putIfAbsent(node, GraphInfo())
    }

    private fun addEdge(source: String, destination: String, fieldName: String, graph: MutableMap<String, GraphInfo>): Boolean? {
        addNode(source, graph)
        addNode(destination, graph)
        graph[source]?.edges?.add(destination)
        return graph[source]?.fields?.add(fieldName)
    }

    /**
     * The number of nodes
     * Note: the count include Query and Mutation
     */
    fun getGraphSize(graph: MutableMap<String, GraphInfo>): Int {
        return graph.size
    }

    /**
     * The number of fields that are pointers to other nodes in the graph.
     * Note: fields in Query and Mutation are considered in this count.
     */
    fun getNbrOfEdges(graph: MutableMap<String, GraphInfo>): Int {
        return graph.values.stream().mapToInt { it.edges.size }.sum()
    }

    /**
     * The number of fields in the type Query or mutation
     */
    fun getNbrQueriesOrMutations(queriesMutationsEntryPoint: String, graph: MutableMap<String, GraphInfo>): Int? {
        return graph[queriesMutationsEntryPoint.toLowerCase()]?.fields?.size
    }

    /**
     * Get the number of fields: the number of fields in all nodes, excluding Query and Mutation
     */
    fun getNbrFields(graph: MutableMap<String, GraphInfo>): MutableList<Int> {
        val nbrFields: MutableList<Int> = mutableListOf()
        for (element in graph.entries) {
            if (element.key == "query" || element.key == "querytype" || element.key == "root" || element.key == "mutation")
                continue
            else
                nbrFields.add(element.value.fields.size)

        }
        return nbrFields
    }

    /**
     * Get all adjacent vertex
     */
    fun getAdjacent(vertex: String, graph: MutableMap<String, GraphInfo>): MutableSet<String>? {
        return graph[vertex]?.edges
    }

    /**
     * Check the existence of a node in the graph
     */
    fun checkNode(node: String, graph: MutableMap<String, GraphInfo>): Boolean {

        graph.keys.forEach {
            if (it.toLowerCase() == node) return true
        }

        return false
    }

    /**
    Get all paths
     */
    fun getAllPaths(visitedVertex: MutableSet<String>, stack: Deque<String>, current: String, graph: MutableMap<String, GraphInfo>, paths: MutableList<List<String>>) {
        visitedVertex.add(current)
        stack.push(current)
        val cycle: MutableList<String> = mutableListOf()

        for (adjacent in getAdjacent(current, graph)!!) {
            if (visitedVertex.contains(adjacent)) {
                cycle.add(adjacent)
                continue
            }

            getAllPaths(visitedVertex, stack, adjacent, graph, paths)

        }
        val x = getAdjacent(current, graph)

        x?.removeAll(cycle)

        if (getAdjacent(current, graph).isNullOrEmpty() || x.isNullOrEmpty()) {
            paths.add(stack.reversed())//current is a leaf
            stack.pop() //backtrack
        } else {
            stack.pop() //backtrack
        }

    }

    /**
     * Get the longest path
     */
    fun longest(paths: MutableList<List<String>>): LongestInfo {
        val longest = LongestInfo()
        paths.forEach {
            if ((it.size) >= longest.size) {
                longest.size = it.size
                longest.path = it
            }
        }
        return longest
    }

    fun getUnionOrInterfaceNbr(tag: String, graph: MutableMap<String, GraphInfo>): Int {
        var comp = 0
        for (element in graph) {
            element.value.fields.forEach { if (it == tag) comp += 1 }
        }
        return comp
    }

}