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


    /**
     * A data structure to store information about a graph
     */
    data class GraphInfo(
            var fields: MutableSet<String> = mutableSetOf(),
            var edges: MutableSet<String> = mutableSetOf()//set to omit redundant nodes

    )

    /**
     * A data structure to store information about a path
     */
    data class PathInfo(
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
    fun getAdjacent(vertex: String, graph: Map<String, GraphInfo>): MutableSet<String>? {
        val x = graph[vertex]?.edges
        x
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
     * Get all paths from an entry point (Query or Mutation)
     */
    fun getAllPathsFromEntryPoint(visitedVertex: MutableSet<String>, stack: Deque<String>, current: String, graph: Map<String, GraphInfo>, paths: MutableList<List<String>>) {
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

        val tempGraph = mapOf<String, GraphInfo>()// the tempGraph is used because the function removeAll() will modify the initial structure graph

        val x = getAdjacent(current, tempGraph)

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
    fun longest(paths: MutableList<List<String>>): PathInfo {
        val longest = PathInfo()
        paths.forEach {
            if ((it.size) >= longest.size) {
                longest.size = it.size
                longest.path = it
            }
        }
        return longest
    }

    /**
     * Get the shortest path
     */
    fun shortestPath(paths: MutableList<List<String>>): PathInfo {//todo refactor with longest
        val shortest = PathInfo()
        if (paths.size == 0) {
            shortest.path = listOf()
            shortest.size = 0
            return shortest
        } else {
            shortest.path = paths.minWith(Comparator.comparingInt { it.size })!!//todo be careful with size 0
            shortest.size = shortest.path.size
            return shortest
        }

        /*  if (paths.size == 0) {
              shortest.path = listOf()
              shortest.size = 0
              return shortest
          } else {
              paths.sortedWith(Comparator.comparingInt { l -> l.size })
              shortest.path = paths[0]
              shortest.size = paths[0].size
              return shortest
          }*/
    }


    fun getUnionOrInterfaceNbr(tag: String, graph: MutableMap<String, GraphInfo>): Int {
        var comp = 0
        for (element in graph) {
            element.value.fields.forEach { if (it == tag) comp += 1 }
        }
        return comp
    }

    /**
     * Get the shortest path from each entry point (i.e. the return objects in each Query and Mutation) to each node
     */
    fun getShortestPathFromEachEntryPointToEachNode(graph: MutableMap<String, GraphInfo>): MutableList<PathInfo> {
        val x: MutableList<PathInfo> = mutableListOf()
        for (entry in graph) {
            if (isRoots(entry)) {
                for (edge in entry.value.edges) {
                    for (node in graph) {
                        if (isRoots(node)|| node.key == edge)
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
     * return all paths from a node x to a node y
     */
    fun getAllPaths(start: String, end: String, graph: MutableMap<String, GraphInfo>): MutableList<List<String>> {
        val stack = ArrayDeque<String>()
        val paths: MutableList<List<String>> = mutableListOf()
        if (!graph.containsKey(start) || !graph.containsKey(end)) {
            return mutableListOf()//todo check 0 size
        }
//        require(start != end)
        allPathsFromXToY(stack, start, end, graph, paths)
        return paths
    }


    /**
     * Find all paths from a vertex X to vertex Y
     */
    private fun allPathsFromXToY(stack: Deque<String>, current: String, end: String, graph: MutableMap<String, GraphInfo>, paths: MutableList<List<String>>) {
        stack.push(current)
        if (isPathTo(stack, end)) {//goal node already reached
            paths.add(stack.reversed())
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
     *  for P_x compute the mimumin P_xm amond ALL the entry points Todo
     */
    fun minPathAmongAllEntryPointsForEachNode(paths: MutableList<PathInfo>): MutableList<PathInfo> {

        val minPaths: MutableList<PathInfo> = mutableListOf()
        val entryPointObjectsNodes: MutableSet<String> = mutableSetOf()//todo check cases
        val onlyPaths: MutableList<List<String>> = mutableListOf()
        val regularNodes: MutableSet<String> = mutableSetOf()

        paths.forEach { if (it.path.isNotEmpty()) onlyPaths.add(it.path) }//extract only path from the structure without the size
        onlyPaths.forEach { entryPointObjectsNodes.add(it.first()) }//extract entry points
        //extract "regular" nodes
        onlyPaths.forEach { i ->
            i.forEach {
                if (!entryPointObjectsNodes.contains(it)) {
                    regularNodes.add(it)
                }
            }
        }
        //find all paths for a node x, then take the min
        regularNodes.forEach { s ->
            val onlyPathsNodeX: MutableList<List<String>> = mutableListOf()
           // findPathsEndingWithVertexX(paths, s).forEach {
            findPathsEndingWithVertexX(onlyPaths, s).forEach {
                onlyPathsNodeX.add(it)
            }
            minPaths.add(shortestPath(onlyPathsNodeX))
        }
        return minPaths
        /*to remove
        val w: MutableList<PathInfo> = mutableListOf()
        //for (entry in graph) {
        //if (entry.key.toLowerCase() == "query" || entry.key.toLowerCase() == "querytype" || entry.key.toLowerCase() == "root" || entry.key.toLowerCase() == "mutation") {
        //for (edge in entry.value.edges) {//for each object in the entry point
        for (node in graph) {//node is query or its objects
            if (node.key.toLowerCase() == "query" || node.key.toLowerCase() == "querytype" || node.key.toLowerCase() == "root" || node.key.toLowerCase() == "mutation" || edge.toLowerCase() == node.key.toLowerCase())
                continue
            //this is a "normal" node
            //find all paths from entry point to this node
            val endWithNode = findPathsEndingWithVertexX(paths, node.key)//the min path
            val onlyPaths: MutableList<List<String>> = mutableListOf()
            endWithNode.forEach { onlyPaths.add(it.path) }

            w.add(shortestPath(onlyPaths))
            //}
            //}
            //}
        }
        return w

        */
    }


    fun findPathsEndingWithVertexX(paths: MutableList<List<String>>, vertex: String): MutableList<List<String>> {//paths here = ShortestFromEachEntryPointToEachNogde

        val endWithVertexX: MutableList<List<String>> = mutableListOf()

        paths.forEach { if (it.isNotEmpty() && it.last() == vertex) endWithVertexX.add((it)) }

        return endWithVertexX

    }


    /**
     *  once we have P_xm for every single node in the graph, compute the Maximum
     */
    fun maxPathAmongAllEntryPointsForAllNodes(paths: MutableList<PathInfo>): PathInfo {//todo refactor with longest path?

        val longest = PathInfo()
        paths.forEach {
            if (it.size >= longest.size) {
                longest.size = it.size
                longest.path = it.path
            }
        }

        return longest
    }

    private fun isRoots(entry: MutableMap.MutableEntry<String, GraphInfo>) = (entry.key.toLowerCase() == "query" || entry.key.toLowerCase() == "querytype" || entry.key.toLowerCase() == "root" || entry.key.toLowerCase() == "mutation")

}