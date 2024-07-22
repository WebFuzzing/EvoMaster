package org.evomaster.core.problem.graphql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.graphql.builder.Table
import org.evomaster.core.problem.graphql.builder.TempState
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.client.Entity

object GraphQLUtils {

    private val log = LoggerFactory.getLogger(GraphQLUtils::class.java)

    /**
     * A data structure to store information about a graph, where each node is composed of:
     * fields: are primitive types and edges
     * edges: are fields pointing to other nodes
     */
    data class GraphInfo(
        var fields: MutableSet<String> = mutableSetOf(),
        var edges: MutableSet<String> = mutableSetOf()//Set to omit redundant nodes

    )

    /**
     * A data structure to store information about a path
     */
    data class PathInfo(
        var size: Int = 0,
        var path: List<String> = listOf()
    )

    fun generateGQLBodyEntity(a: GraphQLAction, targetFormat: OutputFormat): Entity<String>? {

        //TOdo check empty return type
        val returnGene = a.parameters.find { p -> p is GQReturnParam }?.gene

        val inputGenes = a.parameters.filterIsInstance<GQInputParam>().map { it.gene }


        var bodyEntity: Entity<String> = Entity.json(" ")


        if (a.methodType == GQMethodType.QUERY) {

            if (inputGenes.isNotEmpty()) {

                val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes, targetFormat)

                val printableInputGenes = getPrintableInputGenes(printableInputGene)


                if (printableInputGenes.isNotEmpty()) {

                    //primitive type in Return
                    bodyEntity = if (returnGene == null) {
                        Entity.json(
                            """
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)         } ","variables":null}
                """.trimIndent()
                        )

                    } else {
                        val query = getQuery(returnGene, a)
                        Entity.json(
                            """
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)  $query       } ","variables":null}
                """.trimIndent()
                        )

                    }
                } else {// need to remove the ()

                    //primitive type in Return
                    bodyEntity = if (returnGene == null) {
                        Entity.json(
                            """
                    {"query" : "  { ${a.methodName}  $printableInputGenes         } ","variables":null}
                """.trimIndent()
                        )

                    } else {
                        val query = getQuery(returnGene, a)
                        Entity.json(
                            """
                    {"query" : "  { ${a.methodName}  $printableInputGenes  $query       } ","variables":null}
                """.trimIndent()
                        )

                    }


                }
            } else {//request without arguments
                bodyEntity = if (returnGene == null) { //primitive type
                    Entity.json(
                        """
                    {"query" : "  { ${a.methodName}       } ","variables":null}
                """.trimIndent()
                    )

                } else {
                    var query = getQuery(returnGene, a)
                    Entity.json(
                        """
                   {"query" : " {  ${a.methodName}  $query   }   ","variables":null}
                """.trimIndent()
                    )
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
                Entity.json(
                    """
                {"query" : " mutation{ ${a.methodName}  $inputParams         } ","variables":null}
            """.trimIndent()
                )

            } else {
                val mutation = getMutation(returnGene, a)
                Entity.json(
                    """
                { "query" : "mutation{    ${a.methodName}  $inputParams    $mutation    }","variables":null}
            """.trimIndent()
                )

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

    private fun getPrintableInputGene(inputGenes: List<Gene>, targetFormat: OutputFormat? = null): MutableList<String> {
        val printableInputGene = mutableListOf<String>()

        for (gene in inputGenes) {
            //if it is opt , it should be active
            //if(gene.getWrappedGene(OptionalGene::class.java)?.isActive != false)
            if ((gene.getWrappedGene(OptionalGene::class.java)?.isActive == true) || (gene.getWrappedGene(OptionalGene::class.java) == null))
                printableInputGene.add(inputsPrinting(gene, targetFormat))

        }
        return printableInputGene
    }

    /**
     * This function is used to get printable String.
     * Initially used for GQL arguments (eg: GQL input parameters, tuple arguments)
     */
    fun inputsPrinting(
        it: Gene,
        targetFormat: OutputFormat?
    ) = if (it.getWrappedGene(EnumGene::class.java) != null) {//enum gene
        val i = it.getValueAsRawString()
        "${it.name} : $i"
    } else {
        if (it.getWrappedGene(ObjectGene::class.java) != null) {//object gene
            val i = it.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
            //if it is nullable it should be active
            if (it.getWrappedGene(NullableGene::class.java)?.isActive == true || (it.getWrappedGene(NullableGene::class.java) == null))
                " $i"
            else
            //Need the name of the object when it takes "null" as a value, since it will not access the object
            //where the name is printed
                "${it.name} : $i"
        } else {
            if (it.getWrappedGene(ArrayGene::class.java) != null) {//array gene
                val i = it.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE)
                "${it.name} : $i"
            } else {
                /*
                     TODO
                     Man: this is a temporal solution
                     there might also need a further handling, e.g., field of object is String, Array<String>
                         */
                val mode =
                    if (ParamUtil.getValueGene(it) is StringGene) GeneUtils.EscapeMode.GQL_STR_VALUE else GeneUtils.EscapeMode.GQL_INPUT_MODE
                val i = it.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
                "${it.name} : $i"
            }
        }
    }

    fun repairIndividual(ind: GraphQLIndividual) {
        ind.seeAllActions()
            .filterIsInstance<GraphQLAction>()
            .forEach { a ->
                a.parameters.filterIsInstance<GQReturnParam>().forEach { p ->
                    if (p.gene is ObjectGene) {
                        if (p.gene.fields.any {
                                (it is TupleGene && it.lastElementTreatedSpecially) || (it is BooleanGene) || (it is OptionalGene)
                            }) {
                            GeneUtils.repairBooleanSelection(p.gene)
                        }
                    }
                }
            }
    }

    /**
     * Used to construct recursively a graph from the GQL schema. Its takes:
     * state: contains information about the types extracted from the GQL schema,
     * typeName: the name of the type,
     * fieldName: the name of the field,
     * graph: a map with the name of the node as a key and its fields and edges as values,
     * history: used in the cycles management,
     * objectFieldsHistory: a set used in the management of already constructed nodes

     * TODO Remove isRoot(), and extract the names directly from the schema
     */
    fun constructGraph(
        state: TempState,
        typeName: String,
        fieldName: String,
        graph: MutableMap<String, GraphInfo>,
        history: MutableList<String>,
        objectFieldsHistory: MutableSet<String>
    ): MutableMap<String, GraphInfo> {

        for (element in state.tables) {
            val kOTFT = element.kindOfFieldType.toString().toLowerCase()

            if (element.typeName?.toLowerCase() != typeName.toLowerCase()) {
                continue
            }
            if (isRoot(typeName)) {//First entry point
                if (kOTFT == GqlConst.OBJECT) {
                    history.add(element.fieldType)
                    addEdge(element.typeName!!.toLowerCase(), element.fieldType, element.fieldName, graph)
                    getObjectNode(history, element, objectFieldsHistory, state, graph)
                }
                if (isScalarOrEnum(element)) //Primitive type: create a field and add it
                    graph[element.typeName!!.toLowerCase()]?.fields?.add(element.fieldName)

                if (kOTFT == GqlConst.INTERFACE) {
                    history.add(element.fieldType)// add interface type object to the history
                    addEdge(
                        element.typeName!!.toLowerCase(),
                        element.fieldType,
                        element.fieldName,
                        graph
                    )//from a query node to the interface type object
                    getInterfaceNodes(history, element, state, graph, objectFieldsHistory)
                }
                if (kOTFT == GqlConst.UNION) {
                    history.add(element.fieldType)// add union type object to the history
                    addEdge(
                        element.typeName!!.toLowerCase(),
                        element.fieldType,
                        element.fieldName,
                        graph
                    )//from a query node to the union type object
                    getUnionNodes(history, element, graph, state, objectFieldsHistory)
                }

            } else {//Second entry
                if (kOTFT == GqlConst.OBJECT) {
                    history.add(element.fieldType)
                    addEdge(typeName, element.fieldType, element.fieldName, graph)
                    getObjectNode(history, element, objectFieldsHistory, state, graph)
                }
                if (isScalarOrEnum(element)) //Primitive type: create a field and add it
                    graph[typeName]?.fields?.add(element.fieldName)

                if (kOTFT == GqlConst.UNION) {
                    history.add(element.fieldType)// add union type object to the history
                    addEdge(
                        typeName,
                        element.fieldType,
                        element.fieldName,
                        graph
                    )//from a node to the union type object
                    getUnionNodes(history, element, graph, state, objectFieldsHistory)
                }
                if (kOTFT == GqlConst.INTERFACE) {
                    history.add(element.fieldType)// add interface type object to the history
                    addEdge(
                        typeName,
                        element.fieldType,
                        element.fieldName,
                        graph
                    )//from a node to the interface type object
                    getInterfaceNodes(history, element, state, graph, objectFieldsHistory)
                }
            }
        }
        return graph
    }

    private fun isScalarOrEnum(element: Table) =
        element.kindOfFieldType.toString()
            .toLowerCase() == GqlConst.SCALAR || element.kindOfFieldType.toString().toLowerCase() == GqlConst.ENUM

    private fun getInterfaceNodes(
        history: MutableList<String>,
        element: Table,
        state: TempState,
        graph: MutableMap<String, GraphInfo>,
        objectFieldsHistory: MutableSet<String>
    ) {
        if (history.count { it == element.fieldType } <= 1) {// the interface type object already treated
            constructGraph(
                state,
                element.fieldType,
                "",
                graph,
                history,
                objectFieldsHistory
            )//construct the base fields of the interface
            element.interfaceTypes.forEach { ob ->
                history.add(ob)//add the interface objects to the history
                addEdge(
                    element.fieldType,
                    ob,
                    GqlConst.INTERFACE_TAG,
                    graph
                ) // from the interface type object to its objects
                if (history.count { it == ob } <= 1) {
                    constructGraph(state, ob, "", graph, history, objectFieldsHistory)
                    history.remove(ob)
                } else {
                    history.remove(ob)
                }
            }
            history.remove(element.fieldType)// remove the interface type object to the history:the normal case (not cycle)
        } else {
            history.remove(element.fieldType)// remove the interface type object to the history:the cycle case
        }
    }

    private fun getUnionNodes(
        history: MutableList<String>,
        element: Table,
        graph: MutableMap<String, GraphInfo>,
        state: TempState,
        objectFieldsHistory: MutableSet<String>
    ) {
        if (history.count { it == element.fieldType } <= 1) {// the union type object is already treated
            element.unionTypes.forEach { ob ->
                history.add(ob)//add the union objects to the history
                addEdge(
                    element.fieldType,
                    ob,
                    GqlConst.UNION_TAG,
                    graph
                ) // from the union type object to its objects
                if (history.count { it == ob } <= 1) {
                    constructGraph(state, ob, "", graph, history, objectFieldsHistory)
                    history.remove(ob)
                } else {
                    history.remove(ob)
                }
            }
            history.remove(element.fieldType)// remove the union type object to the history: the normal case (not cycle)
        } else {
            history.remove(element.fieldType)// remove the union type object to the history:the cycle case
        }
    }

    private fun getObjectNode(
        history: MutableList<String>,
        element: Table,
        objectFieldsHistory: MutableSet<String>,
        state: TempState,
        graph: MutableMap<String, GraphInfo>
    ) {
        if (history.count { it == element.fieldType } <= 1) {
            if (!objectFieldsHistory.contains(element.fieldType)) {
                constructGraph(state, element.fieldType, element.fieldName, graph, history, objectFieldsHistory)
                history.remove(element.fieldType)
                objectFieldsHistory.add(element.fieldType)
            } else {
                history.remove(element.fieldType)
            }
        } else {
            history.remove(element.fieldType)
        }
    }

    private fun addNode(node: String, graph: MutableMap<String, GraphInfo>): GraphInfo? {
        return graph.putIfAbsent(node, GraphInfo())
    }

    private fun addEdge(
        source: String,
        destination: String,
        fieldName: String,
        graph: MutableMap<String, GraphInfo>
    ): Boolean? {
        addNode(source, graph)
        addNode(destination, graph)
        graph[source]?.edges?.add(destination)
        return graph[source]?.fields?.add(fieldName)
    }

    /**
     * The number of nodes
     * Note: the count include Query and Mutation
     */
    fun getGraphSize(graph: Map<String, GraphInfo>): Int {
        return graph.size
    }

    /**
     * The number of fields that are pointers to other nodes in the graph.
     * Note: fields in Query and Mutation are considered in this count.
     */
    fun getNumberOfEdges(graph: Map<String, GraphInfo>): Int {
        return graph.values.stream().mapToInt { it.edges.size }.sum()
    }

    /**
     * The number of fields in the type Query or mutation
     */
    fun getNumberOfQueriesOrMutations(queriesMutationsEntryPoint: String, graph: Map<String, GraphInfo>): Int? {
        if (queriesMutationsEntryPoint !in graph.keys)
            throw IllegalArgumentException(" Query/Mutation entry points are not in the graph ")
        else return graph[queriesMutationsEntryPoint.toLowerCase()]?.fields?.size
    }

    /**
     * Get the number of fields: the number of fields in all nodes, excluding Query and Mutation
     */
    fun getNumberOfFields(graph: Map<String, GraphInfo>): MutableList<Int> {
        val numberOfFields: MutableList<Int> = mutableListOf()
        for (element in graph.entries) {
            if (isRoot(element))
                continue
            else
                numberOfFields.add(element.value.fields.size)
        }
        return numberOfFields
    }

    /**
     * Get all adjacent vertex
     */
    fun getAdjacent(vertex: String, graph: Map<String, GraphInfo>): MutableSet<String>? {
        return graph[vertex]?.edges
    }

    /**
     * Check the existence of a node in the graph
     */
    fun checkNodeExists(node: String, graph: Map<String, GraphInfo>): Boolean {

        graph.keys.forEach {
            if (it.toLowerCase() == node) return true
        }
        return false
    }

    /**
     * Used to get all paths from an entry point (Query or Mutation), where:
     * visitedVertex: is a set representing already visited vertex
     * stack: is used in the
     * current: is the current vertex
     * graph: is the constructed graph based on the schema
     * paths: are the list of all paths from an entry point (Query/Mutation node) to a leaf
     */
    fun getAllPathsFromEntryPoint(
        visitedVertex: MutableSet<String>,
        stack: Deque<String>,
        current: String,
        graph: Map<String, GraphInfo>,
        paths: MutableList<List<String>>
    ) {
        visitedVertex.add(current)
        stack.push(current)
        val cycle: MutableList<String> = mutableListOf()
        for (adjacent in getAdjacent(current, graph)!!) {
            if (visitedVertex.contains(adjacent)) {
                cycle.add(adjacent)
                continue
            }
            getAllPathsFromEntryPoint(visitedVertex, stack, adjacent, graph, paths)
        }
        //The tempGraph is used because the function removeAll() will modify the initial graph structure
        val tempGraph = mapOf<String, GraphInfo>()

        val adj = getAdjacent(current, tempGraph)

        adj?.removeAll(cycle)

        if (getAdjacent(current, graph).isNullOrEmpty() || adj.isNullOrEmpty()) {
            //note JDK 21 added a reversed() method to Deque, which leads to issues...
            paths.add(stack.toList().reversed())//current is a leaf
            stack.pop() //backtrack
        } else {
            stack.pop() //backtrack
        }
    }

    /**
     * Get the longest path
     */
    fun longestPath(paths: List<List<String>>): List<String> {
        return paths.maxWithOrNull(Comparator.comparingInt { it.size })!!
    }

    /**
     * Get the shortest path
     */
    fun shortestPath(paths: List<List<String>>): List<String> {
        return if (paths.isEmpty()) {
            listOf()
        } else {
            paths.minWithOrNull(Comparator.comparingInt { it.size })!!
        }
    }


    fun getNumberOfUnionOrInterface(tag: String, graph: Map<String, GraphInfo>): Int {
        var comp = 0
        for (element in graph) {
            element.value.fields.forEach { if (it == tag) comp += 1 }
        }
        return comp
    }

    /**
     * Get the minimum path from each entry point (i.e. the return objects in each Query and Mutation) to each node
     */
    fun getShortestPathFromEachEntryPointToEachNode(graph: Map<String, GraphInfo>): MutableList<List<String>> {
        val x: MutableList<List<String>> = mutableListOf()
        for (entry in graph) {
            if (isRoot(entry)) {
                for (edge in entry.value.edges) {
                    for (node in graph) {
                        if (isRoot(node) || node.key == edge)
                            continue
                        //this is a "regular" node
                        //find all paths from entry point to this node
                        x.add(shortestPath(getAllPaths(edge, node.key, graph)))
                    }
                }
            }
        }
        return x
    }


    /**
     * Return all paths from a node x to a node y
     */
    fun getAllPaths(start: String, end: String, graph: Map<String, GraphInfo>): MutableList<List<String>> {
        val stack = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        if (!graph.containsKey(start) || !graph.containsKey(end)) {
            return mutableListOf()
        }
        //require(start != end)//depends on previous checks
        allPathsFromXToY(stack, start, end, graph, paths)
        return paths
    }


    /**
     * Find all paths from a vertex X to vertex Y
     */
    private fun allPathsFromXToY(
        stack: Deque<String>,
        current: String,
        end: String,
        graph: Map<String, GraphInfo>,
        paths: MutableList<List<String>>
    ) {
        stack.push(current)
        if (isPathTo(stack, end)) {//goal node already reached
            paths.add(stack.toList().reversed())
        }
        for (adjacent in getAdjacent(current, graph)!!) {
            if (stack.contains(adjacent)) {
                continue
            }
            allPathsFromXToY(stack, adjacent, end, graph, paths)
            stack.pop()//backtrack
        }
    }

    /**
     * Return true if there is a path to this vertex
     */
    fun isPathTo(stack: Deque<String>, vertex: String): Boolean {
        return !stack.isEmpty() && stack.peek() == vertex
    }

    /**
     *  Get the minimum path, among minimum path from each entry point (return objects in each Query and Mutation) to each node
     */
    fun minPathAmongAllEntryPointsForEachNode(paths: List<List<String>>): MutableList<List<String>> {
        val minPaths: MutableList<List<String>> = mutableListOf()
        val entryPointObjectsNodes: MutableSet<String> = mutableSetOf()//TODO check cases
        val regularNodes: MutableSet<String> = mutableSetOf()

        paths.forEach { if (it.isNotEmpty()) entryPointObjectsNodes.add(it.first()) }//extract entry points
        //extract "regular" nodes
        paths.forEach { i ->
            i.forEach {
                if (!entryPointObjectsNodes.contains(it)) {
                    regularNodes.add(it)
                }
            }
        }
        //find all paths for a node x, then take the min
        regularNodes.forEach { s ->
            minPaths.add(shortestPath(findPathsEndingWithVertexX(paths, s)))
        }
        return minPaths
    }


    fun findPathsEndingWithVertexX(paths: List<List<String>>, vertex: String): MutableList<List<String>> {
        val endWithVertexX: MutableList<List<String>> = mutableListOf()
        paths.forEach { if (it.isNotEmpty() && it.last() == vertex) endWithVertexX.add((it)) }
        return endWithVertexX

    }

    /**
     *  Get the max path of all minimum path among minimum path from each entry point (return objects in each Query and Mutation) to each node
     */
    fun maxPathAmongAllEntryPointsForAllNodes(paths: MutableList<List<String>>): PathInfo {

        val maxPath = PathInfo()
        maxPath.path = longestPath(paths)
        maxPath.size = maxPath.path.size
        return maxPath
    }

    private fun isRoot(entry: Map.Entry<String, GraphInfo>) = isRoot(entry.key)

    private fun isRoot(entryPoint: String) =
        entryPoint.toLowerCase() == GqlConst.QUERY || entryPoint.toLowerCase() == GqlConst.QUERY_TYPE || entryPoint.toLowerCase() == GqlConst.ROOT || entryPoint.toLowerCase() == GqlConst.MUTATION

}