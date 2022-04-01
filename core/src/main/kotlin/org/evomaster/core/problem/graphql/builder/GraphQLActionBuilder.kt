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
     * TODO potential for refactoring, as not thread-safe
     */
    private var accum: Int = 0
    /*
      In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
      */
    private val mutationQueryConstants = listOf(GqlConst.MUTATION, GqlConst.QUERY, GqlConst.ROOT, GqlConst.QUERY_TYPE)


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
        val schemaObj: SchemaObj = try {
            Gson().fromJson(schema, SchemaObj::class.java)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse the schema of the SUT as a JSON object: ${e.message}")
        }

        val state = StateBuilder.initTablesInfo(schemaObj)

        if (schemaObj.data.__schema.queryType != null || schemaObj.data.__schema.mutationType != null) {
            for (element in state.tables) {

                if (mutationQueryConstants.contains(element.typeName.lowercase())) {
                    handleOperation(state, actionCluster, treeDepth, element)
                }
            }
        } else {
            throw SutProblemException("The given GraphQL schema has no Query nor Mutation operation")
        }
    }


    private fun handleOperation(
        state: TempState,
        actionCluster: MutableMap<String, Action>,
        maxNumberOfGenes: Int,
        element: Table
    ) {
        val type = when {
            element.typeName.equals(GqlConst.QUERY, true) -> GQMethodType.QUERY
            /*
               In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
                */
            element.typeName.equals(GqlConst.ROOT, true) -> GQMethodType.QUERY
            element.typeName.equals(GqlConst.QUERY_TYPE, true) -> GQMethodType.QUERY
            element.typeName.equals(GqlConst.MUTATION, true) -> GQMethodType.MUTATION
            else -> {
                log.warn("GraphQL Entry point: ${element.typeName} is not found.")
                return
            }
        }

        val actionId = "${element.fieldName}${idGenerator.incrementAndGet()}"

        val params = extractParams(
            state,
            maxNumberOfGenes,
            element
        )

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

        /*
       In some cases object gene (optional or not) with all fields as cycle object gene (optional or not) are generated.
       So we need to deactivate it by looking into its ancestors (e.g., Optional set to false, Array set length to 0)
        */
        params.map { it.gene }.forEach {

            when {
                it is ObjectGene -> it.flatView().forEach { g ->
                    if (g is OptionalGene && g.gene is ObjectGene) handleAllCyclesInObjectFields(g.gene) else if (g is ObjectGene) handleAllCyclesInObjectFields(
                        g
                    )
                }
                it is OptionalGene && it.gene is ObjectGene -> it.flatView().forEach { g ->
                    if (g is OptionalGene && g.gene is ObjectGene) handleAllCyclesInObjectFields(g.gene) else if (g is ObjectGene) handleAllCyclesInObjectFields(
                        g
                    )
                }
                it is ArrayGene<*> && it.template is ObjectGene -> it.flatView().forEach { g ->
                    it.template.fields.forEach { f ->
                        if (f is OptionalGene && f.gene is ObjectGene) handleAllCyclesInObjectFields(
                            f.gene
                        ) else if (f is ObjectGene) handleAllCyclesInObjectFields(f)
                    }
                }
                it is OptionalGene && it.gene is ArrayGene<*> && it.gene.template is ObjectGene -> it.flatView()
                    .forEach { g ->
                        it.gene.template.fields.forEach { f ->
                            if (f is OptionalGene && f.gene is ObjectGene) handleAllCyclesInObjectFields(
                                f.gene
                            ) else if (f is ObjectGene) handleAllCyclesInObjectFields(f)
                        }
                    }
            }
        }


        /*
        prevent LimitObjectGene
         */
        params.map { it.gene }.forEach { GeneUtils.preventLimit(it, true) }

        //Create the action
        val action = GraphQLAction(actionId, element.fieldName, type, params)
        actionCluster[action.getName()] = action

    }

    fun handleAllCyclesInObjectFields(gene: ObjectGene) {

        if (gene.fields.all {
                (it is OptionalGene && it.gene is CycleObjectGene) ||
                        (it is CycleObjectGene)

            }) {
            GeneUtils.tryToPreventSelection(gene)
        }
    }


    private fun extractParams(
        state: TempState,
        maxNumberOfGenes: Int,
        element: Table

    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque()
        val selectionInArgs = state.argsTablesIndexedByName[element.fieldName] ?: listOf()

        if (element.isFieldNameWithArgs) {

            for (element in selectionInArgs) {
                if (element.kindOfFieldType == SCALAR.toString() || element.kindOfFieldType == ENUM.toString()) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                    val gene = getInputScalarListOrEnumListGene(
                        state,
                        history,
                        element
                    )
                    params.add(GQInputParam(element.fieldName, gene))

                } else {//for input objects types and objects types
                    val gene = getInputGene(
                        state,
                        history,
                        maxNumberOfGenes,
                        element
                    )
                    params.add(GQInputParam(element.fieldName, gene))
                }
            }

            //handling the return param, should put all the fields optional
            val gene = getReturnGene(
                state,
                history,
                accum,
                maxNumberOfGenes,
                element
            )

            //Remove primitive types (scalar and enum) from return params
            if (isReturnNotPrimitive(gene)) params.add(GQReturnParam(element.fieldName, gene))

        } else {
            //The action does not contain arguments, it only contains a return type
            //in handling the return param, should put all the fields optional
            val gene = getReturnGene(
                state,
                history,
                accum,
                maxNumberOfGenes,
                element
            )
            //Remove primitive types (scalar and enum) from return params
            if (isReturnNotPrimitive(gene)) params.add(GQReturnParam(element.fieldName, gene))
        }

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
        history: Deque<String>,
        element: Table
    ): Gene {
        when (element.KindOfFieldName.lowercase()) {
            GqlConst.LIST ->
                return if (element.isKindOfFieldNameOptional) {
                    val copy = element.copy(
                        fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                        kindOfFieldType = element.KindOfFieldName,
                        typeName = element.fieldType
                    )
                    val template = getInputScalarListOrEnumListGene(state, history, copy)
                    OptionalGene(element.fieldName, ArrayGene(element.fieldName, template))
                } else {
                    val copy = element.copy(
                        fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                        kindOfFieldType = element.KindOfFieldName,
                        typeName = element.fieldType
                    )
                    val template = getInputScalarListOrEnumListGene(state, history, copy)
                    ArrayGene(element.fieldName, template)
                }
            "int" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, IntegerGene(element.fieldName))
                else
                    IntegerGene(element.fieldName)
            "string" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, StringGene(element.fieldName))
                else
                    StringGene(element.fieldName)
            "float" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, FloatGene(element.fieldName))
                else
                    FloatGene(element.fieldName)
            "boolean" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, BooleanGene(element.fieldName))
                else
                    BooleanGene(element.fieldName)
            "long" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, LongGene(element.fieldName))
                else
                    LongGene(element.fieldName)
            "null" -> {
                val copy = element.copy(
                    fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                    kindOfFieldType = element.KindOfFieldName,
                    typeName = element.fieldType
                )
                return getInputScalarListOrEnumListGene(state, history, copy)
            }
            "date" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, BooleanGene(element.fieldName))
                else
                    DateGene(element.fieldName)
            GqlConst.SCALAR -> {
                val copy = element.copy(
                    KindOfFieldName = element.typeName,
                    typeName = element.KindOfFieldName
                )
                return getInputScalarListOrEnumListGene(state, history, copy)
            }
            "id" ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, StringGene(element.fieldName))
                else
                    StringGene(element.fieldName)
            GqlConst.ENUM ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, EnumGene(element.fieldName, element.enumValues))
                else
                    EnumGene(element.fieldName, element.enumValues)

            GqlConst.UNION -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support union in input type: ${element.KindOfFieldName}")
                return StringGene("Not supported type")
            }
            GqlConst.INTERFACE -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support union in input type: ${element.KindOfFieldName}")
                return StringGene("Not supported type")
            }
            else ->
                return if (element.isKindOfFieldTypeOptional)
                    OptionalGene(element.fieldName, StringGene(element.fieldName))
                else
                    StringGene(element.fieldName)
        }
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
                    val optObjGene = createObjectGene(state, history, accum, maxNumberOfGenes, element)
                    OptionalGene(element.fieldName, optObjGene)
                } else
                    createObjectGene(state, history, accum, maxNumberOfGenes, element)
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
                    kindOfFieldType = element.kindOfFieldType,
                    typeName = element.fieldName,
                    isKindOfFieldTypeOptional = element.isKindOfFieldTypeOptional,
                    isKindOfFieldNameOptional = element.isKindOfFieldNameOptional,
                    enumValues = element.enumValues,
                    unionTypes = element.unionTypes,
                    interfaceTypes = element.interfaceTypes,
                    isFieldNameWithArgs = element.isFieldNameWithArgs,
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
                            kindOfFieldType = element.kindOfFieldType,
                            typeName = element.fieldName,
                            isKindOfFieldTypeOptional = element.isKindOfFieldTypeOptional,
                            isKindOfFieldNameOptional = element.isKindOfFieldNameOptional,
                            enumValues = element.enumValues,
                            unionTypes = element.unionTypes,
                            interfaceTypes = element.interfaceTypes,
                            isFieldNameWithArgs = element.isFieldNameWithArgs
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
            fieldType = element.fieldType,
            KindOfFieldName = element.KindOfFieldName,
            kindOfFieldType = element.kindOfFieldType,
            typeName = element.fieldType,
            isKindOfFieldTypeOptional = secondElement.isKindOfFieldTypeOptional,
            isKindOfFieldNameOptional = secondElement.isKindOfFieldNameOptional,
            enumValues = element.enumValues,
            unionTypes = element.unionTypes,
            interfaceTypes = element.interfaceTypes,
            isFieldNameWithArgs = element.isFieldNameWithArgs
        )
    }

    /**
     * Extract the return gene: representing the return value in the GQL query/mutation.
     * From an implementation point of view, it represents a GQL return param. In contrast to input param, we can have only one return param.
     */
    private fun getReturnGene(
        state: TempState,
        history: Deque<String>,
        accum: Int,
        maxNumberOfGenes: Int,
        element: Table
    ): Gene {

        var accum = accum
        val initAccum =
            accum // needed since we restore the accumulator in the interface after we construct the #Base# object

        when (element.KindOfFieldName.lowercase()) {
            GqlConst.LIST -> {
                val copy = element.copy(
                    fieldType = element.typeName, KindOfFieldName = element.kindOfFieldType,
                    kindOfFieldType = element.KindOfFieldName,
                    typeName = element.fieldType,
                )
                val template = getReturnGene(state, history, accum, maxNumberOfGenes, copy)

                return OptionalGene(element.fieldName, ArrayGene(element.fieldName, template))
            }
            GqlConst.OBJECT -> {
                accum += 1
                return if (checkDepth(accum, maxNumberOfGenes)) {
                    history.addLast(element.typeName)
                    if (history.count { it == element.typeName } == 1) {
                        val objGene = createObjectGene(
                            state, history, accum, maxNumberOfGenes, element
                        )
                        history.removeLast()
                        OptionalGene(element.fieldName, objGene)
                    } else {
                        history.removeLast()
                        (OptionalGene(element.fieldName, CycleObjectGene(element.fieldName)))
                    }

                } else {
                    OptionalGene(element.fieldName, LimitObjectGene(element.fieldName))
                }
            }
            GqlConst.UNION -> {
                history.addLast(element.typeName)
                return if (history.count { it == element.typeName } == 1) {
                    val optObjGene = createUnionObjectsGene(
                        state,
                        history,
                        accum,
                        maxNumberOfGenes,
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

                    accum += 1
                    if (checkDepth(accum, maxNumberOfGenes)) {
                        //will contain basic interface fields, and had as name the methode name
                        var interfaceBaseOptObjGene = createObjectGene(
                            state, history, accum, maxNumberOfGenes, element
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
                            maxNumberOfGenes,
                            element
                        )

                        //merge basic interface fields with additional interface fields
                        interfaceAdditionalOptObjGene.add(
                            OptionalGene(
                                element.fieldName + GqlConst.INTERFACE_BASE_TAG,
                                interfaceBaseOptObjGene
                            )
                        )

                        //will return a single optional object gene with optional basic interface fields and optional additional interface fields
                        OptionalGene(
                            element.fieldName + GqlConst.INTERFACE_TAG,
                            ObjectGene(element.fieldName + GqlConst.INTERFACE_TAG, interfaceAdditionalOptObjGene)
                        )
                    } else {
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
                return getReturnGene(state, history, accum, maxNumberOfGenes, copy)
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
        maxNumberOfGenes: Int,
        element: Table
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        var accum = accum

        /*Look after each field (not tuple) and construct it recursively
          */

        val selection = state.tablesIndexedByName[element.typeName] ?: listOf()


        for (tableElement in selection) {

            val selectionInArgs = state.argsTablesIndexedByName[tableElement.fieldName] ?: listOf()
            /*
            Contains the elements of a tuple
             */
            val tupleElements: MutableList<Gene> = mutableListOf()

            /*
            The field is with arguments (it is a tuple):
             construct its arguments (n-1 elements) and;
             its last element (return)
             */
            if (tableElement.isFieldNameWithArgs) {
                /*
             Construct field s arguments (the n-1 elements of the tuple) first
             */
                for (argElement in selectionInArgs) {
                    if (argElement.kindOfFieldType == SCALAR.toString() || argElement.kindOfFieldType == ENUM.toString()) {
                        /*
                    array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                     */
                        val gene = getInputScalarListOrEnumListGene(
                            state,
                            history,
                            argElement
                        )
                        /*
                    Adding one element to the tuple
                     */
                        tupleElements.add(gene)
                    } else {
                        /*
                    for input objects types and objects types
                     */
                        val gene = getInputGene(
                            state,
                            history,
                            maxNumberOfGenes,
                            argElement
                        )
                        tupleElements.add(gene)
                    }

                }
                /*
             Construct the last element (the return)
             */
                constructReturn(
                    state,
                    tableElement,
                    history,
                    element.isKindOfFieldTypeOptional,
                    element.isKindOfFieldNameOptional,
                    accum,
                    maxNumberOfGenes,
                    tupleElements
                )

                /*
                The the tuple field is constructed
                put it into an optional ? todo
             */
                val constructedTuple =
                    OptionalGene(tupleElements.last().name, TupleGene(tupleElements.last().name, tupleElements))
                fields.add(constructedTuple)

            } else
            /*
            The field is without arguments.
            regular object field (the return)
             */
                constructReturn(
                    state,
                    tableElement,
                    history,
                    element.isKindOfFieldTypeOptional,
                    element.isKindOfFieldNameOptional,
                    accum,
                    maxNumberOfGenes,
                    fields
                )
        }

        return ObjectGene(element.fieldName, fields)
    }

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
                fieldType = tableElement.fieldType,
                KindOfFieldName = tableElement.KindOfFieldName,
                kindOfFieldType = tableElement.kindOfFieldType,
                typeName = tableElement.fieldType,
                isKindOfFieldTypeOptional = isKindOfTableFieldTypeOptional,
                isKindOfFieldNameOptional = isKindOfTableFieldOptional,
                enumValues = tableElement.enumValues,
                unionTypes = tableElement.unionTypes,
                interfaceTypes = tableElement.interfaceTypes,
                isFieldNameWithArgs = tableElement.isFieldNameWithArgs
            )
            val gene = getReturnGene(state, history, accum, maxNumberOfGenes, copy)
            tupleElements.add(gene)
        } else
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
            if (checkDepth(accum, maxNumberOfGenes)) {
                history.addLast(elementInUnionTypes)
                val copy = element.copy(
                    typeName = elementInUnionTypes,
                    kindOfFieldType = element.kindOfFieldType,
                    isKindOfFieldTypeOptional = element.isKindOfFieldTypeOptional,
                    isKindOfFieldNameOptional = element.isKindOfFieldNameOptional,
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
            if (checkDepth(accum, maxNumberOfGenes)) {
                history.addLast(elementInInterfaceTypes)
                val copy = element.copy(
                    typeName = elementInInterfaceTypes,
                    kindOfFieldType = element.kindOfFieldType,
                    isKindOfFieldTypeOptional = element.isKindOfFieldTypeOptional,
                    isKindOfFieldNameOptional = element.isKindOfFieldNameOptional,
                    fieldName = elementInInterfaceTypes
                )
                val objGeneTemplate = createObjectGene(state, history, accum, maxNumberOfGenes, copy)


                history.removeLast()

                var myObjGne = objGeneTemplate as ObjectGene//To object

                myObjGne = myObjGne.copyFields(interfaceBaseOptObjGene as ObjectGene)//remove useless fields

                if (myObjGne.fields.isNotEmpty())
                    fields.add(OptionalGene(myObjGne.name, myObjGne))
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


    private fun checkDepth(count: Int, maxNumberOfGenes: Int): Boolean {
        return count <= maxNumberOfGenes
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



