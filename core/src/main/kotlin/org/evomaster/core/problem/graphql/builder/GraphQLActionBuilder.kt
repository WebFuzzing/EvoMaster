package org.evomaster.core.problem.graphql.builder

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.core.problem.graphql.GqlConst
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.graphql.schema.*
import org.evomaster.core.problem.graphql.schema.__TypeKind.*
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.datetime.DateGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object GraphQLActionBuilder {

    private val log: Logger = LoggerFactory.getLogger(GraphQLActionBuilder::class.java)

    /**
     * Used to create unique IDs
     */
    private val idGenerator = AtomicInteger()

    /**
     * Cache to avoid rebuilding the same genes again and again.
     *
     * Key: id of element in schema
     * Value: a gene for it
     *
     * Note: this is NOT thread-safe
     */
    private val cache = mutableMapOf<String, Gene>()

    private val depthBasedCache = mutableMapOf<String, Gene>()

    /**
     * @param schema: the schema extracted from a GraphQL API, as a JSON string
     * @param actionCluster: for each mutation/query in the schema, populate this map with
     *                      new action templates.
     * @param treeDepth:  maximum depth for the created objects, to avoid HUGE data structures
     */
    fun addActionsFromSchema(
        schema: String,
        actionCluster: MutableMap<String, Action>,
        treeDepth: Int = Int.MAX_VALUE  //no restrictions by default
    ) {
        cache.clear()
        depthBasedCache.clear()

        val schemaObj: SchemaObj = try {
            Gson().fromJson(schema, SchemaObj::class.java)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse the schema of the SUT as a JSON object: ${e.message}")
        }

        val state = StateBuilder.initTablesInfo(schemaObj)

        if (schemaObj.data.__schema.queryType != null || schemaObj.data.__schema.mutationType != null) {
            for (element in state.tables) {
                if (schemaObj.data.__schema.queryType?.name?.toLowerCase() == element.typeName.lowercase()) {
                    handleOperation(state, actionCluster, treeDepth, element, GQMethodType.QUERY)
                } else if (schemaObj.data.__schema.mutationType?.name?.toLowerCase() == element.typeName.lowercase()) {
                    handleOperation(state, actionCluster, treeDepth, element, GQMethodType.MUTATION)
                }
            }
        } else {
            throw SutProblemException("The given GraphQL schema has no Query nor Mutation operation")
        }
    }


    private fun handleOperation(
        state: TempState,
        actionCluster: MutableMap<String, Action>,
        treeDepth: Int,
        element: Table,
        type: GQMethodType
    ) {

        val actionId = "${element.fieldName}${idGenerator.incrementAndGet()}"

        /*
            For this operation, we extract the inputs (if any) and return object (if any) as
            parameter that the search can evolve
         */
        val params = extractParams(state, treeDepth, element)

        //Note: if a return param is a primitive type it will be null

        //Get the boolean selection of the constructed return param
        val returnGene = params.find { p -> p is GQReturnParam }?.gene
        val selection = returnGene?.let { GeneUtils.getBooleanSelection(it) }

        //remove the constructed return param
        params.remove(params.find { p -> p is GQReturnParam })
        //add constructed return param selection instead
        selection?.name?.let { GQReturnParam(it, selection) }?.let { params.add(it) }

        /*
            all fields are optional in GraphQL, so should always be possible to prevent cycles,
            unless the schema is wrong (eg, must still satisfy that at least one field is selected)
         */
        params.map { it.gene }.forEach { GeneUtils.preventCycles(it, true) }

        params.map { it.gene }.forEach { GeneUtils.preventLimit(it, true) }

        /*
      In some cases object gene (optional or not) with all fields as cycle object gene (optional or not) are generated.
      Also, in some cases (ex: in AnigList) object gene (optional or not) with all fields as tuples gene with all their last elements
      as kind of Limit gene are generated.
      So we need to deactivate it by looking into its ancestors (e.g., Optional set to false, Array set length to 0)
       */
        handleAllCyclesAndLimitInObjectFields(params)

        //Create the action
        val action = GraphQLAction(actionId, element.fieldName, type, params)
        actionCluster[action.getName()] = action

    }

    private fun handleAllCyclesAndLimitInObjectFields(
        params: MutableList<Param>
    ) {
        params.map { it.gene }.forEach {
            when {
                it is ObjectGene -> it.flatView().forEach { g ->
                    if (g is OptionalGene && g.gene is ObjectGene) handleAllCyclesAndLimitInObjectFields(g.gene)
                    else if (g is ObjectGene) handleAllCyclesAndLimitInObjectFields(
                        g
                    )
                }
                it is OptionalGene && it.gene is ObjectGene -> it.flatView().forEach { g ->
                    if (g is OptionalGene && g.gene is ObjectGene) handleAllCyclesAndLimitInObjectFields(g.gene) else if (g is ObjectGene) handleAllCyclesAndLimitInObjectFields(
                        g
                    )
                }
                it is ArrayGene<*> && it.template is ObjectGene -> it.flatView().forEach { g ->
                    it.template.fields.forEach { f ->
                        if (f is OptionalGene && f.gene is ObjectGene) handleAllCyclesAndLimitInObjectFields(
                            f.gene
                        ) else if (f is ObjectGene) handleAllCyclesAndLimitInObjectFields(f)
                    }
                }
                it is OptionalGene && it.gene is ArrayGene<*> && it.gene.template is ObjectGene -> it.flatView()
                    .forEach { g ->
                        it.gene.template.fields.forEach { f ->
                            if (f is OptionalGene && f.gene is ObjectGene) handleAllCyclesAndLimitInObjectFields(
                                f.gene
                            ) else if (f is ObjectGene) handleAllCyclesAndLimitInObjectFields(f)
                        }
                    }
            }
        }
    }

    fun handleAllCyclesAndLimitInObjectFields(gene: ObjectGene) {
        if (gene.fields.all {
                ((it is CycleObjectGene)) ||
                        (it is OptionalGene && it.gene is CycleObjectGene) ||
                        (it is LimitObjectGene) ||
                        ((it is OptionalGene && it.gene is LimitObjectGene) ||
                                ((it is OptionalGene && it.gene is LimitObjectGene) || (it is OptionalGene && it.gene is ObjectGene && it.gene.fields.all { it is OptionalGene && it.gene is LimitObjectGene })) ||
                                /*
                                The Object gene contains: All fields as tuples, need to check if theirs last elements are kind of limit gene.
                                If it is the case, need to prevent selection on them
                                */
                                (it is TupleGene && it.lastElementTreatedSpecially && isLastContainsAllLimitGenes(it)) ||
                                (it is OptionalGene && it.gene is TupleGene && it.gene.lastElementTreatedSpecially && isLastContainsAllLimitGenes(
                                    it.gene
                                )))

            }) {
            GeneUtils.tryToPreventSelection(gene)
        }
    }

    /*
    Check if the last element in the tuple (the return) either is:
    optional limit gene, optional Object with all fields as limit gene or optional Object with all fields as Optional limit gene
     */
    private fun isLastContainsAllLimitGenes(tuple: TupleGene): Boolean {
        val lastElement = tuple.elements.last()
        return (lastElement is OptionalGene && lastElement is LimitObjectGene) ||
                (lastElement is OptionalGene && lastElement.gene is ObjectGene && lastElement.gene.fields.all { it is LimitObjectGene }
                        ) || (lastElement is OptionalGene && lastElement.gene is ObjectGene && lastElement.gene.fields.all { it is OptionalGene && it.gene is LimitObjectGene }
                )
    }

    private fun extractParams(
        state: TempState,
        maxNumberOfGenes: Int,
        element: Table
    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val selectionInArgs = state.argsTablesIndexedByName[element.fieldName] ?: listOf()

        if (element.isFieldNameWithArgs) {
            for (input in selectionInArgs) {
                if (input.kindOfFieldType == SCALAR.toString() || input.kindOfFieldType == ENUM.toString()) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                    val gene = getInputScalarListOrEnumListGene(state, input)
                    params.add(GQInputParam(input.fieldName, gene))
                } else {//for input objects types and objects types
                    val gene = getInputGene(
                        state,
                        ArrayDeque(),
                        maxNumberOfGenes,
                        input
                    )
                    params.add(GQInputParam(input.fieldName, gene))
                }
            }
        }

        //handling the return param, should put all the fields optional
        val gene = getReturnGene(
            state,
            ArrayDeque(),
            0,
            maxNumberOfGenes,
            element
        )

        //Remove primitive types (scalar and enum) from return params
        if (isReturnNotPrimitive(gene)) params.add(GQReturnParam(element.fieldName, gene))

        return params
    }

    private fun isReturnNotPrimitive(
        gene: Gene,
    ) = (gene.name.lowercase() != "scalar"
            && !(gene is OptionalGene && gene.gene.name == "scalar")
            && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.name.lowercase() == "scalar")
            && !(gene is ArrayGene<*> && gene.template.name.lowercase() == "scalar")
            && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.name.lowercase() == "scalar")
            && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template.name.lowercase() == "scalar")
            //enum cases
            && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.gene is EnumGene<*>)
            && !(gene is ArrayGene<*> && gene.template is EnumGene<*>)
            && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.gene is EnumGene<*>)
            && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is EnumGene<*>)
            && gene !is EnumGene<*>
            && !(gene is OptionalGene && gene.gene is EnumGene<*>))


    /**Note: There are tree functions containing blocs of "when": two functions for inputs and one for return.
     *For the inputs: blocs of "when" could not be refactored since they are different because the names are different.
     *And for the return: it could not be refactored with inputs because we do not consider optional/non optional cases (unlike in inputs) .
     */


    /**
     * For Scalar arrays types and Enum arrays types
     */
    private fun getInputScalarListOrEnumListGene(
        state: TempState,
        element: Table
    ): Gene {

        val gene = cache[element.uniqueId]
        if (gene != null) {
            return gene.copy()
        }

        val created: Gene = when (element.KindOfFieldName.lowercase()) {
            GqlConst.LIST ->
                if (element.isKindOfFieldNameOptional) {
                    val copy = element.copy(
                        fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                        kindOfFieldType = element.KindOfFieldName,
                        typeName = element.fieldType
                    )
                    val template = getInputScalarListOrEnumListGene(state, copy)
                    OptionalGene(element.fieldName, ArrayGene(element.fieldName, template))
                } else {
                    val copy = element.copy(
                        fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                        kindOfFieldType = element.KindOfFieldName,
                        typeName = element.fieldType
                    )
                    val template = getInputScalarListOrEnumListGene(state, copy)
                    ArrayGene(element.fieldName, template)
                }
            "int" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, IntegerGene(element.fieldName))
                else
                    IntegerGene(element.fieldName)
            "string" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, StringGene(element.fieldName))
                else
                    StringGene(element.fieldName)
            "float" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, FloatGene(element.fieldName))
                else
                    FloatGene(element.fieldName)
            "boolean" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, BooleanGene(element.fieldName))
                else
                    BooleanGene(element.fieldName)
            "long" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, LongGene(element.fieldName))
                else
                    LongGene(element.fieldName)
            "null" -> {
                val copy = element.copy(
                    fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                    kindOfFieldType = element.KindOfFieldName,
                    typeName = element.fieldType
                )
                getInputScalarListOrEnumListGene(state, copy)
            }
            "date" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, BooleanGene(element.fieldName))
                else
                    DateGene(element.fieldName)
            GqlConst.SCALAR -> {
                val copy = element.copy(
                    KindOfFieldName = element.typeName,
                    typeName = element.KindOfFieldName
                )
                getInputScalarListOrEnumListGene(state, copy)
            }
            "id" ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, StringGene(element.fieldName))
                else
                    StringGene(element.fieldName)
            GqlConst.ENUM ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, EnumGene(element.fieldName, element.enumValues))
                else
                    EnumGene(element.fieldName, element.enumValues)

            GqlConst.UNION -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support union in input type: ${element.KindOfFieldName}")
                StringGene("Not supported type")
            }
            GqlConst.INTERFACE -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support union in input type: ${element.KindOfFieldName}")
                StringGene("Not supported type")
            }
            else ->
                if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, StringGene(element.fieldName))
                else
                    StringGene(element.fieldName)
        }

        cache[element.uniqueId] = created
        return created
    }


    /**
     * Used to extract the input gene: representing arguments in the GQL query/mutation.
     * From an implementation point of view, it represents a GQL input param. we can have 0 or n argument for one action.
     */
    private fun getInputGene(
        state: TempState,
        history: Deque<String>,
        maxNumberOfGenes: Int,
        element: Table
    ): Gene {

        when (element.KindOfFieldName.lowercase()) {
            GqlConst.LIST ->
                return if (element.isKindOfFieldNameOptional) {
                    val copy = element.copy(
                        fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                        kindOfFieldType = element.KindOfFieldName, typeName = element.fieldType,
                    )
                    val template = getInputGene(state, history, maxNumberOfGenes, copy)

                    OptionalGene(element.fieldName, ArrayGene(element.fieldName, template))
                } else {
                    val copy = element.copy(
                        fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                        kindOfFieldType = element.KindOfFieldName, typeName = element.fieldType
                    )
                    val template = getInputGene(state, history, maxNumberOfGenes, copy)

                    ArrayGene(element.fieldName, template)
                }
            GqlConst.OBJECT ->
                return if (element.isKindOfFieldTypeOptional) {
                    val optObjGene = createObjectGene(state, history, 0, maxNumberOfGenes, element)
                    OptionalGene(element.fieldName, optObjGene)
                } else
                    createObjectGene(state, history, 0, maxNumberOfGenes, element)
            GqlConst.INPUT_OBJECT ->
                return if (element.isKindOfFieldTypeOptional) {
                    val optInputObjGene = createInputObjectGene(state, history, maxNumberOfGenes, element)
                    OptionalGene(element.fieldName, optInputObjGene)
                } else
                    createInputObjectGene(state, history, maxNumberOfGenes, element)

            "int" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, IntegerGene(element.typeName))
                else
                    IntegerGene(element.typeName)
            "string" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, StringGene(element.typeName))
                else
                    StringGene(element.typeName)
            "float" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, FloatGene(element.typeName))
                else
                    FloatGene(element.typeName)
            "boolean" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, BooleanGene(element.typeName))
                else
                    BooleanGene(element.typeName)
            "long" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, LongGene(element.typeName))
                else
                    LongGene(element.typeName)
            "null" -> {
                val copy = element.copy(
                    fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                    kindOfFieldType = element.KindOfFieldName, typeName = element.fieldType,
                )
                return getInputGene(state, history, maxNumberOfGenes, copy)

            }
            "date" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, DateGene(element.typeName))
                else
                    DateGene(element.typeName)
            GqlConst.ENUM ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, EnumGene(element.typeName, element.enumValues))
                else
                    EnumGene(element.typeName, element.enumValues)
            GqlConst.SCALAR -> {
                val copy = element.copy(
                    fieldType = element.fieldType, KindOfFieldName = element.typeName,
                    typeName = element.KindOfFieldName
                )
                return getInputGene(state, history, maxNumberOfGenes, copy)
            }
            "id" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, StringGene(element.typeName))
                else
                    StringGene(element.typeName)

            GqlConst.UNION -> {
                LoggingUtil.uniqueWarn(log, " GQL does not support union in input type: ${element.KindOfFieldName}")
                return StringGene("Not supported type")
            }
            GqlConst.INTERFACE -> {
                LoggingUtil.uniqueWarn(
                    log,
                    "GQL does not support interface in input type: ${element.KindOfFieldName}"
                )
                return StringGene("Not supported type")
            }
            else ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.typeName, StringGene(element.typeName))
                else
                    StringGene(element.typeName)

        }
    }

    /**
     * Create input object gene
     */
    private fun createInputObjectGene(
        state: TempState,
        history: Deque<String>,
        maxNumberOfGenes: Int,
        element: Table
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        val selectionInArgs = state.argsTablesIndexedByName[element.typeName] ?: listOf()
        for (element in selectionInArgs) {
            if (element.kindOfFieldType.lowercase() == GqlConst.SCALAR) {
                val copy = element.copy(
                    fieldType = element.typeName,
                    KindOfFieldName = element.fieldType,
                    typeName = element.fieldName
                )

                val template = getInputGene(state, history, maxNumberOfGenes, copy)
                fields.add(template)
            } else
                if (element.KindOfFieldName.lowercase() == GqlConst.LIST) {
                    val copy = copyTableElement(element, element)
                    val template = getInputGene(state, history, maxNumberOfGenes, copy)

                    fields.add(template)
                } else
                    if (element.kindOfFieldType.lowercase() == GqlConst.INPUT_OBJECT) {
                        val copy = copyTableElement(element, element)
                        val template = getInputGene(state, history, maxNumberOfGenes, copy)

                        fields.add(template)

                    } else if (element.kindOfFieldType.lowercase() == GqlConst.ENUM) {
                        val copy = element.copy(
                            fieldType = element.typeName,
                            KindOfFieldName = element.kindOfFieldType,
                            typeName = element.fieldName
                        )
                        val template = getInputGene(state, history, maxNumberOfGenes, copy)

                        fields.add(template)
                    }
        }
        return ObjectGene(element.fieldName, fields)
    }

    private fun copyTableElement(
        element: Table,
        secondElement: Table
    ): Table {
        return element.copy(
            typeName = element.fieldType,
            isKindOfFieldTypeOptional = secondElement.isKindOfFieldTypeOptional,
            isKindOfFieldNameOptional = secondElement.isKindOfFieldNameOptional
        )
    }

    /**
     * Extract the return gene: representing the return value in the GQL query/mutation.
     * From an implementation point of view, it represents a GQL return param. In contrast to input param, we can have only one return param.
     */
    private fun getReturnGene(
        state: TempState,
        history: Deque<String>,
        initAccum: Int,
        treeDepth: Int,
        element: Table
    ): Gene {


        when (element.KindOfFieldName.lowercase()) {
            GqlConst.LIST -> {
                val copy = element.copy(
                    fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                    kindOfFieldType = element.KindOfFieldName,
                    typeName = element.fieldType,
                )
                val template = getReturnGene(state, history, initAccum, treeDepth, copy)

                return OptionalGene(element.fieldName, ArrayGene(element.fieldName, template))
            }
            GqlConst.OBJECT -> {
                val accum = initAccum + 1
                return if (checkDepthIsOK(accum, treeDepth)) {
                    history.addLast(element.typeName)
                    if (history.count { it == element.typeName } == 1) {

                        val id = "$accum:${element.uniqueId}"
                        val gene = depthBasedCache[id]
                        val objGene = if (gene != null) {
                            gene.copy() as ObjectGene
                        } else {
                            val g = createObjectGene(state, history, accum, treeDepth, element)
                            depthBasedCache[id] = g
                            g
                        }
                        history.removeLast()
                        OptionalGene(element.fieldName, objGene)
                    } else {
                        //we have a cycle, in which same object has been seen in ancestor
                        history.removeLast()
                        (OptionalGene(element.fieldName, CycleObjectGene(element.fieldName)))
                    }

                } else {
                    //we reached the limit of depth we want to have in the created objects
                    OptionalGene(element.fieldName, LimitObjectGene(element.fieldName))
                }
            }
            GqlConst.UNION -> {
                history.addLast(element.typeName)
                return if (history.count { it == element.typeName } == 1) {
                    val optObjGene = createUnionObjectsGene(
                        state,
                        history,
                        initAccum,
                        treeDepth,
                        element
                    )
                    history.removeLast()
                    OptionalGene(element.fieldName + GqlConst.UNION_TAG, optObjGene)
                } else {
                    history.removeLast()
                    (OptionalGene(element.fieldName, CycleObjectGene(element.fieldName)))
                }
            }
            GqlConst.INTERFACE -> {
                history.addLast(element.typeName)

                return if (history.count { it == element.typeName } == 1) {

                    var accum = initAccum + 1
                    if (checkDepthIsOK(accum, treeDepth)) {
                        //will contain basic interface fields, and had as name the methode name
                        var interfaceBaseOptObjGene = createObjectGene(
                            state, history, accum, treeDepth, element
                        )
                        interfaceBaseOptObjGene = interfaceBaseOptObjGene as ObjectGene

                        interfaceBaseOptObjGene.name = interfaceBaseOptObjGene.name.plus(GqlConst.INTERFACE_BASE_TAG)

                        accum = initAccum //because #Base# and additional interface fields are in the same level

                        //will contain additional interface fields, and had as name the name of the objects
                        val interfaceAdditionalOptObjGene = createInterfaceObjectGene(
                            state,
                            history,
                            interfaceBaseOptObjGene,
                            accum,
                            treeDepth,
                            element
                        )

                        //merge basic interface fields with additional interface fields
                        interfaceAdditionalOptObjGene.add(
                            OptionalGene(
                                element.fieldName + GqlConst.INTERFACE_BASE_TAG,
                                interfaceBaseOptObjGene
                            )
                        )
                        history.removeLast()
                        //will return a single optional object gene with optional basic interface fields and optional additional interface fields
                        OptionalGene(
                            element.fieldName + GqlConst.INTERFACE_TAG,
                            ObjectGene(element.fieldName + GqlConst.INTERFACE_TAG, interfaceAdditionalOptObjGene)
                        )
                    } else {
                        history.removeLast()
                        OptionalGene(element.fieldName, LimitObjectGene(element.fieldName))
                    }
                } else {
                    history.removeLast()
                    (OptionalGene(element.fieldName, CycleObjectGene(element.fieldName)))
                }
            }
            "null" -> {
                val copy = element.copy(
                    fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                    kindOfFieldType = element.KindOfFieldName,
                    typeName = element.fieldType
                )
                return getReturnGene(state, history, initAccum, treeDepth, copy)
            }
            GqlConst.ENUM ->
                return createEnumGene(
                    element.KindOfFieldName,
                    element.enumValues,
                )
            GqlConst.SCALAR ->
                return createScalarGene(
                    element.typeName,
                    element.KindOfFieldName,
                )
            else ->
                return OptionalGene(element.fieldName, StringGene(element.fieldName))
        }
    }

    private fun createObjectGene(
        state: TempState,
        /**
         * This history store the names of the object, union and interface types (i.e. tableFieldType in Table.kt ).
         * It is used in cycles managements (detecting cycles due to object, union and interface types).
         */
        history: Deque<String>,
        accum: Int,
        maxTreeDepth: Int,
        element: Table
    ): ObjectGene {
        val fields: MutableList<Gene> = mutableListOf()

        //Look after each field (not tuple) and construct it recursively
        val selection = state.tablesIndexedByName[element.typeName] ?: listOf()

        for (tableElement in selection) {

            val selectionInArgs = state.argsTablesIndexedByName[tableElement.fieldName] ?: listOf()

            //Contains the elements of a tuple
            val tupleElements: MutableList<Gene> = mutableListOf()

            /*
            The field is with arguments (it is a tuple): construct its arguments (n-1 elements) and;
             its last element (return)
             */
            if (tableElement.isFieldNameWithArgs) {

                //Construct field s arguments (the n-1 elements of the tuple) first
                for (argElement in selectionInArgs) {
                    if (argElement.kindOfFieldType == SCALAR.toString() || argElement.kindOfFieldType == ENUM.toString()) {
                        //array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                        val gene = getInputScalarListOrEnumListGene(state, argElement)
                        tupleElements.add(gene)
                    } else {
                        //for input objects types and objects types
                        val gene = getInputGene(
                            state,
                            ArrayDeque(history),
                            maxTreeDepth,
                            argElement
                        )
                        tupleElements.add(gene)
                    }
                }
                //Construct the last element (the return)
                constructReturn(
                    state,
                    tableElement,
                    ArrayDeque(history),
                    element.isKindOfFieldTypeOptional,
                    element.isKindOfFieldNameOptional,
                    accum,
                    maxTreeDepth,
                    tupleElements
                )

                val constructedTuple = if (isLastNotPrimitive(tupleElements.last()))
                    OptionalGene(
                        tupleElements.last().name, TupleGene(
                            tupleElements.last().name, tupleElements,
                            lastElementTreatedSpecially = true
                        )
                    )
                else
                //Dropping the last element since it is a primitive type
                    OptionalGene(
                        tupleElements.last().name, TupleGene(
                            tupleElements.last().name, tupleElements.dropLast(1),
                            lastElementTreatedSpecially = false
                        )
                    )

                fields.add(constructedTuple)

            } else
            /*
            The field is without arguments.
            regular object field (the return)
             */
                constructReturn(
                    state,
                    tableElement,
                    ArrayDeque(history),
                    element.isKindOfFieldTypeOptional,
                    element.isKindOfFieldNameOptional,
                    accum,
                    maxTreeDepth,
                    fields
                )
        }

        return ObjectGene(element.fieldName, fields)
    }

    private fun isLastNotPrimitive(lastElements: Gene) = ((lastElements is ObjectGene) ||
            ((lastElements is OptionalGene) && (lastElements.gene is ObjectGene)) ||
            ((lastElements is ArrayGene<*>) && (lastElements.template is ObjectGene)) ||
            ((lastElements is ArrayGene<*>) && (lastElements.template is OptionalGene) && (lastElements.template.gene is ObjectGene)) ||
            ((lastElements is OptionalGene) && (lastElements.gene is ArrayGene<*>) && (lastElements.gene.template is ObjectGene)) ||
            ((lastElements is OptionalGene) && (lastElements.gene is ArrayGene<*>) && (lastElements.gene.template is OptionalGene) && (lastElements.gene.template.gene is ObjectGene))
            )

    private fun constructReturn(
        state: TempState,
        tableElement: Table,
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        accum: Int,
        maxNumberOfGenes: Int,
        tupleElements: MutableList<Gene>
    ) {

        if (tableElement.KindOfFieldName.lowercase() == GqlConst.LIST) {
            val copy = tableElement.copy(
                typeName = tableElement.fieldType,
                isKindOfFieldTypeOptional = isKindOfTableFieldTypeOptional,
                isKindOfFieldNameOptional = isKindOfTableFieldOptional
            )
            val gene = getReturnGene(state, history, accum, maxNumberOfGenes, copy)
            tupleElements.add(gene)
            return
        }

        when (tableElement.kindOfFieldType.lowercase()) {
            GqlConst.OBJECT -> {
                val copy = tableElement.copy(
                    isKindOfFieldTypeOptional = isKindOfTableFieldTypeOptional,
                    isKindOfFieldNameOptional = isKindOfTableFieldOptional
                )
                val gene = getReturnGene(state, history, accum, maxNumberOfGenes, copy)
                tupleElements.add(gene)
            }
            GqlConst.SCALAR -> {
                val gene = createScalarGene(
                    tableElement.fieldType,
                    tableElement.fieldName,
                )
                tupleElements.add(gene)
            }
            GqlConst.ENUM -> {
                val gene = createEnumGene(
                    tableElement.fieldName,
                    tableElement.enumValues
                )
                tupleElements.add(gene)
            }
            GqlConst.UNION -> {
                val copy = tableElement.copy(
                    isKindOfFieldTypeOptional = isKindOfTableFieldTypeOptional,
                    isKindOfFieldNameOptional = isKindOfTableFieldOptional
                )
                val template = getReturnGene(state, history, accum, maxNumberOfGenes, copy)

                tupleElements.add(template)

            }
            GqlConst.INTERFACE -> {
                val copy = tableElement.copy(
                    isKindOfFieldTypeOptional = isKindOfTableFieldTypeOptional,
                    isKindOfFieldNameOptional = isKindOfTableFieldOptional
                )
                val template = getReturnGene(state, history, accum, maxNumberOfGenes, copy)

                tupleElements.add(template)
            }
        }
    }

    /**
     * Create the correspondent object gene for each object defining the union type
     */
    private fun createUnionObjectsGene(
        state: TempState,
        history: Deque<String>,
        accum: Int,
        maxNumberOfGenes: Int,
        element: Table
    ): Gene {

        val fields: MutableList<Gene> = mutableListOf()

        var accum = accum
        val initAccum =
            accum // needed since we restore the accumulator each time we construct one object defining the union

        for (elementInUnionTypes in element.unionTypes) {//Browse all objects defining the union
            accum += 1
            if (checkDepthIsOK(accum, maxNumberOfGenes)) {
                history.addLast(elementInUnionTypes)
                val copy = element.copy(
                    typeName = elementInUnionTypes,
                    fieldName = elementInUnionTypes
                )

                val objGeneTemplate = createObjectGene(state, history, accum, maxNumberOfGenes, copy)

                history.removeLast()
                fields.add(OptionalGene(objGeneTemplate.name, objGeneTemplate))
            } else {
                fields.add(OptionalGene(elementInUnionTypes, LimitObjectGene(elementInUnionTypes)))
            }
            accum = initAccum
        }
        return ObjectGene(element.fieldName + GqlConst.UNION_TAG, fields)
    }

    private fun createInterfaceObjectGene(
        state: TempState,
        history: Deque<String>,
        interfaceBaseOptObjGene: Gene,
        accum: Int,
        maxNumberOfGenes: Int,
        element: Table

    ): MutableList<Gene> {

        val fields: MutableList<Gene> = mutableListOf()

        var accum = accum
        val initAccum =
            accum // needed since we restore the accumulator each time we construct one object defining the interface

        for (elementInInterfaceTypes in element.interfaceTypes) {//Browse all additional objects in the interface
            accum += 1
            if (checkDepthIsOK(accum, maxNumberOfGenes)) {
                history.addLast(elementInInterfaceTypes)
                val copy = element.copy(
                    typeName = elementInInterfaceTypes,
                    fieldName = elementInInterfaceTypes
                )
                var objGeneTemplate = createObjectGene(state, history, accum, maxNumberOfGenes, copy)

                history.removeLast()

                objGeneTemplate =
                    objGeneTemplate.copyFields(interfaceBaseOptObjGene as ObjectGene)//remove useless fields

                if (objGeneTemplate.fields.isNotEmpty())
                    fields.add(OptionalGene(objGeneTemplate.name, objGeneTemplate))
            } else {
                fields.add(
                    OptionalGene(
                        elementInInterfaceTypes,
                        LimitObjectGene(elementInInterfaceTypes)
                    )
                )
            }
            accum = initAccum
        }

        return fields
    }


    private fun checkDepthIsOK(count: Int, maxTreeDepth: Int): Boolean {
        return count <= maxTreeDepth
    }

    fun createScalarGene(
        kindOfTableField: String?,
        tableType: String,
    ): Gene {

        when (kindOfTableField?.lowercase()) {
            "int" ->
                return OptionalGene(tableType, IntegerGene(tableType))
            "string" ->
                return OptionalGene(tableType, StringGene(tableType))
            "float" ->
                return OptionalGene(tableType, FloatGene(tableType))
            "boolean" ->
                return OptionalGene(tableType, BooleanGene(tableType))
            "long" ->
                return OptionalGene(tableType, LongGene(tableType))
            "date" ->
                return OptionalGene(tableType, DateGene(tableType))
            "id" ->
                return OptionalGene(tableType, StringGene(tableType))
            else ->
                return OptionalGene(tableType, StringGene(tableType))
        }

    }

    private fun createEnumGene(
        tableType: String,
        enumValues: List<String>,
    ): Gene {

        return OptionalGene(tableType, EnumGene(tableType, enumValues))

    }

}



