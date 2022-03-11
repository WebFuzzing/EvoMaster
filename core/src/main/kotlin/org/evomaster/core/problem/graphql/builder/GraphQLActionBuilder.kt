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
     * TODO potential for refactring, as not thread-safe
     */
    private var accum: Int = 0




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

        val state  = StateBuilder.initTablesInfo(schemaObj)

        if (schemaObj.data.__schema.queryType != null || schemaObj.data.__schema.mutationType != null) {
            for (element in state.tables) {
                /*
                In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
                 */
                if (element.typeName?.lowercase() == GqlConst.MUTATION || element.typeName?.lowercase() == GqlConst.QUERY || element.typeName?.lowercase() == GqlConst.ROOT || element?.typeName?.lowercase() == GqlConst.QUERY_TYPE) {
                    handleOperation(
                            state,
                            actionCluster,
                            element.fieldName,
                            element.typeName,
                            element.tableFieldType,
                            element.kindOfTableFieldType.toString(),
                            element.kindOfTableField.toString(),
                            element.typeName.toString(),
                            element.isKindOfTableFieldTypeOptional,
                            element.isKindOfTableFieldOptional,
                            element.tableFieldWithArgs,
                            element.enumValues,
                            element.unionTypes,
                            element.interfaceTypes,
                            treeDepth,
                    )
                }
            }
        } else {
            throw SutProblemException("The given GraphQL schema has no Query nor Mutation operation")
        }
    }



    private fun handleOperation(
            state: TempState,
            actionCluster: MutableMap<String, Action>,
            methodName: String?,
            methodType: String?,
            tableFieldType: String,
            kindOfTableFieldType: String,
            kindOfTableField: String?,
            tableType: String,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            tableFieldWithArgs: Boolean,
            enumValues: MutableList<String>,
            unionTypes: MutableList<String>,
            interfaceTypes: MutableList<String>,
            maxNumberOfGenes: Int
    ) {
        if (methodName == null) {
            log.warn("Skipping operation, as no method name is defined.")
            return
        }
        if (methodType == null) {
            log.warn("Skipping operation, as no method type is defined.")
            return
        }
        val type = when {
            methodType.equals(GqlConst.QUERY, true) -> GQMethodType.QUERY
            /*
               In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
                */
            methodType.equals(GqlConst.ROOT, true) -> GQMethodType.QUERY
            methodType.equals(GqlConst.QUERY_TYPE, true) -> GQMethodType.QUERY
            methodType.equals(GqlConst.MUTATION, true) -> GQMethodType.MUTATION
            else -> {
                log.warn("GraphQL Entry point: $methodType is not found.")
                return
            }
        }

        val actionId = "$methodName${idGenerator.incrementAndGet()}"

        val params = extractParams(
                state, methodName, tableFieldType, kindOfTableFieldType, kindOfTableField,
                tableType, isKindOfTableFieldTypeOptional,
                isKindOfTableFieldOptional, tableFieldWithArgs, enumValues, unionTypes, interfaceTypes, maxNumberOfGenes
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
        val action = GraphQLAction(actionId, methodName, type, params)
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
            methodName: String,
            tableFieldType: String,
            kindOfTableFieldType: String,
            kindOfTableField: String?,
            tableType: String,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            tableFieldWithArgs: Boolean,
            enumValues: MutableList<String>,
            unionTypes: MutableList<String>,
            interfaceTypes: MutableList<String>,
            maxNumberOfGenes: Int

    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque()

        if (tableFieldWithArgs) {

            for (element in state.argsTables) {

                if (element.typeName == methodName) {

                    if (element.kindOfTableFieldType == SCALAR || element.kindOfTableFieldType == ENUM) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                        val gene = getInputScalarListOrEnumListGene(
                                state,
                                element.tableFieldType,
                                element.kindOfTableField.toString(),
                                element.kindOfTableFieldType.toString(),
                                element.typeName.toString(),
                                history,
                                element.isKindOfTableFieldTypeOptional,
                                element.isKindOfTableFieldOptional,
                                element.enumValues,
                                element.fieldName,
                                element.tableFieldWithArgs
                        )
                        params.add(GQInputParam(element.fieldName, gene))

                    } else {//for input objects types and objects types
                        val gene = getInputGene(
                                state,
                                element.tableFieldType,
                                element.kindOfTableField.toString(),
                                element.kindOfTableFieldType.toString(),
                                element.typeName.toString(),
                                history,
                                element.isKindOfTableFieldTypeOptional,
                                element.isKindOfTableFieldOptional,
                                element.enumValues,
                                element.fieldName,
                                element.unionTypes,
                                element.interfaceTypes, maxNumberOfGenes,
                                element.tableFieldWithArgs
                        )
                        params.add(GQInputParam(element.fieldName, gene))
                    }
                }
            }

            //handling the return param, should put all the fields optional
            val gene = getReturnGene(
                    state,
                    tableFieldType,
                    kindOfTableField,
                    kindOfTableFieldType,
                    tableType,
                    history,
                    isKindOfTableFieldTypeOptional,
                    isKindOfTableFieldOptional,
                    enumValues,
                    methodName,
                    unionTypes,
                    interfaceTypes,
                    accum,
                    maxNumberOfGenes,
                    tableFieldWithArgs
            )

            //Remove primitive types (scalar and enum) from return params
            if (isReturnNotPrimitive(gene)) params.add(GQReturnParam(methodName, gene))

        } else {
            //The action does not contain arguments, it only contains a return type
            //in handling the return param, should put all the fields optional
            val gene = getReturnGene(
                    state,
                    tableFieldType,
                    kindOfTableField,
                    kindOfTableFieldType,
                    tableType,
                    history,
                    isKindOfTableFieldTypeOptional,
                    isKindOfTableFieldOptional,
                    enumValues,
                    methodName,
                    unionTypes,
                    interfaceTypes,
                    accum,
                    maxNumberOfGenes,
                    tableFieldWithArgs
            )
            //Remove primitive types (scalar and enum) from return params
            if (isReturnNotPrimitive(gene)) params.add(GQReturnParam(methodName, gene))
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
            && !(gene is EnumGene<*>)
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
            tableFieldType: String,
            kindOfTableField: String?,
            kindOfTableFieldType: String,
            tableType: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            enumValues: MutableList<String>,
            methodName: String,
            tableFieldWithArgs: Boolean
    ): Gene {

        when (kindOfTableField?.lowercase()) {
            GqlConst.LIST ->
                return if (isKindOfTableFieldOptional) {
                    val template = getInputScalarListOrEnumListGene(
                            state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName,
                            tableFieldWithArgs
                    )
                    OptionalGene(methodName, ArrayGene(tableType, template))
                } else {
                    val template = getInputScalarListOrEnumListGene(
                            state,
                            tableType,
                            kindOfTableFieldType,
                            kindOfTableField,
                            tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            enumValues,
                            methodName,
                            tableFieldWithArgs
                    )
                    ArrayGene(methodName, template)
                }
            "int" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, IntegerGene(methodName))
                else
                    IntegerGene(methodName)
            "string" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, StringGene(methodName))
                else
                    StringGene(methodName)
            "float" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, FloatGene(methodName))
                else
                    FloatGene(methodName)
            "boolean" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, BooleanGene(methodName))
                else
                    BooleanGene(methodName)
            "long" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, LongGene(methodName))
                else
                    LongGene(methodName)
            "null" ->
                return getInputScalarListOrEnumListGene(
                        state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName,
                        tableFieldWithArgs
                )
            "date" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, BooleanGene(methodName))
                else
                    DateGene(methodName)
            GqlConst.SCALAR ->
                return getInputScalarListOrEnumListGene(
                        state, tableFieldType, tableType, kindOfTableFieldType, kindOfTableField, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName,
                        tableFieldWithArgs
                )
            "id" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, StringGene(methodName))
                else
                    StringGene(methodName)
            GqlConst.ENUM ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, EnumGene(methodName, enumValues))
                else
                    EnumGene(methodName, enumValues)

            GqlConst.UNION -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support union in input type: $kindOfTableField")
                return StringGene("Not supported type")
            }
            GqlConst.INTERFACE -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support union in input type: $kindOfTableField")
                return StringGene("Not supported type")
            }
            else ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, StringGene(methodName))
                else
                    StringGene(methodName)

        }
    }


    /**
     * Used to extract the input gene: representing arguments in the GQL query/mutation.
     * From an implementation point of view, it represents a GQL input param. we can have 0 or n argument for one action.
     */
    private fun getInputGene(
            state: TempState,
            tableFieldType: String,
            kindOfTableField: String?,
            kindOfTableFieldType: String,
            tableType: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            enumValues: MutableList<String>,
            methodName: String,
            unionTypes: MutableList<String>,
            interfaceTypes: MutableList<String>,
            maxNumberOfGenes: Int,
            tableFieldWithArgs: Boolean
    ): Gene {

        when (kindOfTableField?.lowercase()) {
            GqlConst.LIST ->
                return if (isKindOfTableFieldOptional) {

                    val template = getInputGene(
                            state,
                            tableType,
                            kindOfTableFieldType,
                            kindOfTableField,
                            tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            enumValues,
                            methodName,
                            unionTypes,
                            interfaceTypes, maxNumberOfGenes,
                            tableFieldWithArgs
                    )

                    OptionalGene(methodName, ArrayGene(tableType, template))
                } else {

                    val template = getInputGene(
                            state,
                            tableType,
                            kindOfTableFieldType,
                            kindOfTableField,
                            tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            enumValues,
                            methodName,
                            unionTypes,
                            interfaceTypes, maxNumberOfGenes,
                            tableFieldWithArgs
                    )

                    ArrayGene(methodName, template)
                }
            GqlConst.OBJECT ->
                return if (isKindOfTableFieldTypeOptional) {
                    val optObjGene = createObjectGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, accum,
                            maxNumberOfGenes
                    )
                    OptionalGene(methodName, optObjGene)
                } else
                    createObjectGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, accum,
                            maxNumberOfGenes
                    )
            GqlConst.INPUT_OBJECT ->
                return if (isKindOfTableFieldTypeOptional) {
                    val optInputObjGene = createInputObjectGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, maxNumberOfGenes
                    )
                    OptionalGene(methodName, optInputObjGene)
                } else
                    createInputObjectGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, maxNumberOfGenes
                    )
            "int" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, IntegerGene(tableType))
                else
                    IntegerGene(tableType)
            "string" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, StringGene(tableType))
                else
                    StringGene(tableType)
            "float" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, FloatGene(tableType))
                else
                    FloatGene(tableType)
            "boolean" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, BooleanGene(tableType))
                else
                    BooleanGene(tableType)
            "long" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, LongGene(tableType))
                else
                    LongGene(tableType)
            "null" ->
                return getInputGene(
                        state,
                        tableType,
                        kindOfTableFieldType,
                        kindOfTableField,
                        tableFieldType,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
                        enumValues,
                        methodName,
                        unionTypes,
                        interfaceTypes, maxNumberOfGenes,
                        tableFieldWithArgs
                )
            "date" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, DateGene(tableType))
                else
                    DateGene(tableType)
            GqlConst.ENUM ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, EnumGene(tableType, enumValues))
                else
                    EnumGene(tableType, enumValues)
            GqlConst.SCALAR ->
                return getInputGene(
                        state,
                        tableFieldType,
                        tableType,
                        kindOfTableFieldType,
                        kindOfTableField,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
                        enumValues,
                        methodName,
                        unionTypes,
                        interfaceTypes, maxNumberOfGenes,
                        tableFieldWithArgs
                )
            "id" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, StringGene(tableType))
                else
                    StringGene(tableType)

            GqlConst.UNION -> {
                LoggingUtil.uniqueWarn(log, " GQL does not support union in input type: $kindOfTableField")
                return StringGene("Not supported type")
            }
            GqlConst.INTERFACE -> {
                LoggingUtil.uniqueWarn(log, "GQL does not support interface in input type: $kindOfTableField")
                return StringGene("Not supported type")
            }
            else ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(tableType, StringGene(tableType))
                else
                    StringGene(tableType)

        }
    }

    /**
     * Create input object gene
     */
    private fun createInputObjectGene(
            state: TempState,
            tableType: String,
            kindOfTableFieldType: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            methodName: String,
            maxNumberOfGenes: Int
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        for (element in state.argsTables) {
            if (element.typeName == tableType) {

                if (element.kindOfTableFieldType.toString().lowercase() == GqlConst.SCALAR) {
                    val field = element.fieldName
                    val template = getInputGene(
                            state,
                            tableType,
                            element.tableFieldType,
                            kindOfTableFieldType,
                            field,
                            history,
                            element.isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            element.enumValues,
                            methodName,
                            element.unionTypes,
                            element.interfaceTypes, maxNumberOfGenes,
                            element.tableFieldWithArgs
                    )
                    fields.add(template)
                } else {
                    if (element.kindOfTableField.toString().lowercase() == GqlConst.LIST) {
                        val template = getInputGene(
                                state,
                                element.tableFieldType,
                                element.kindOfTableField.toString(),
                                element.kindOfTableFieldType.toString(),
                                element.tableFieldType,
                                history,
                                isKindOfTableFieldTypeOptional,
                                isKindOfTableFieldOptional,
                                element.enumValues,
                                element.fieldName,
                                element.unionTypes,
                                element.interfaceTypes, maxNumberOfGenes,
                                element.tableFieldWithArgs
                        )

                        fields.add(template)
                    } else
                        if (element.kindOfTableFieldType.toString().lowercase() == GqlConst.INPUT_OBJECT) {
                            val template = getInputGene(
                                    state,
                                    element.tableFieldType,
                                    element.kindOfTableFieldType.toString(),
                                    element.kindOfTableField.toString(),
                                    element.tableFieldType,
                                    history,
                                    isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    element.enumValues,
                                    element.fieldName,
                                    element.unionTypes,
                                    element.interfaceTypes, maxNumberOfGenes,
                                    element.tableFieldWithArgs
                            )

                            fields.add(template)

                        } else if (element.kindOfTableFieldType.toString().lowercase() == GqlConst.ENUM) {
                            val field = element.fieldName
                            val template = getInputGene(
                                    state,
                                    tableType,
                                    element.kindOfTableFieldType.toString(),
                                    kindOfTableFieldType,
                                    field,
                                    history,
                                    element.isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    element.enumValues,
                                    methodName,
                                    element.unionTypes,
                                    element.interfaceTypes, maxNumberOfGenes,
                                    element.tableFieldWithArgs
                            )
                            fields.add(template)
                        }
                }

            }
        }
        return ObjectGene(methodName, fields, tableType)
    }

    /**
     * Extract the return gene: representing the return value in the GQL query/mutation.
     * From an implementation point of view, it represents a GQL return param. In contrast to input param, we can have only one return param.
     */
    private fun getReturnGene(
            state: TempState,
            tableFieldType: String,
            kindOfTableField: String?,
            kindOfTableFieldType: String,
            tableType: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            enumValues: MutableList<String>,
            methodName: String,
            unionTypes: MutableList<String>,
            interfaceTypes: MutableList<String>,
            accum: Int,
            maxNumberOfGenes: Int,
            tableFieldWithArgs: Boolean
    ): Gene {


        var accum = accum
        val initAccum =
                accum // needed since we restore the accumulator in the interface after we construct the #Base# object

        when (kindOfTableField?.lowercase()) {
            GqlConst.LIST -> {
                val template = getReturnGene(
                        state,
                        tableType,
                        kindOfTableFieldType,
                        kindOfTableField,
                        tableFieldType,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
                        enumValues,
                        methodName,
                        unionTypes,
                        interfaceTypes,
                        accum,
                        maxNumberOfGenes,
                        tableFieldWithArgs
                )

                return OptionalGene(methodName, ArrayGene(tableType, template))//check the name
            }
            GqlConst.OBJECT -> {
                accum += 1
                return if (checkDepth(accum, maxNumberOfGenes)) {
                    history.addLast(tableType)
                    if (history.count { it == tableType } == 1) {
                        val objGene = createObjectGene(
                                state, tableType, kindOfTableFieldType, history,
                                isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, accum,
                                maxNumberOfGenes
                        )
                        history.removeLast()
                        OptionalGene(methodName, objGene)
                    } else {
                        history.removeLast()
                        (OptionalGene(methodName, CycleObjectGene(methodName)))
                    }

                } else {
                    OptionalGene(tableType, LimitObjectGene(tableType))
                }
            }
            GqlConst.UNION -> {
                history.addLast(tableType)
                return if (history.count { it == tableType } == 1) {
                    val optObjGene = createUnionObjectsGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, unionTypes, accum,
                            maxNumberOfGenes
                    )
                    history.removeLast()
                    OptionalGene(methodName + GqlConst.UNION_TAG, optObjGene)
                } else {
                    history.removeLast()
                    (OptionalGene(methodName, CycleObjectGene(methodName)))
                }
            }
            GqlConst.INTERFACE -> {
                history.addLast(tableType)

                return if (history.count { it == tableType } == 1) {

                    accum += 1
                    if (checkDepth(accum, maxNumberOfGenes)) {

                        //will contain basic interface fields, and had as name the methode name
                        var interfaceBaseOptObjGene = createObjectGene(
                                state, tableType, kindOfTableFieldType, history,
                                isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, accum,
                                maxNumberOfGenes
                        )

                        interfaceBaseOptObjGene = interfaceBaseOptObjGene as ObjectGene

                        interfaceBaseOptObjGene.name = interfaceBaseOptObjGene.name.plus(GqlConst.INTERFACE_BASE_TAG)

                        accum = initAccum //because #Base# and additional interface fields are in the same level

                        //will contain additional interface fields, and had as name the name of the objects
                        val interfaceAdditionalOptObjGene = createInterfaceObjectGene(
                                state,
                                kindOfTableFieldType,
                                history,
                                isKindOfTableFieldTypeOptional,
                                isKindOfTableFieldOptional,
                                interfaceTypes,
                                interfaceBaseOptObjGene,
                                accum, maxNumberOfGenes
                        )

                        //merge basic interface fields with additional interface fields
                        interfaceAdditionalOptObjGene.add(
                                OptionalGene(
                                        methodName + GqlConst.INTERFACE_BASE_TAG,
                                        interfaceBaseOptObjGene
                                )
                        )

                        //will return a single optional object gene with optional basic interface fields and optional additional interface fields
                        OptionalGene(
                                methodName + GqlConst.INTERFACE_TAG,
                                ObjectGene(methodName + GqlConst.INTERFACE_TAG, interfaceAdditionalOptObjGene)
                        )
                    } else {
                        OptionalGene(tableType, LimitObjectGene(tableType))
                    }
                } else {
                    history.removeLast()
                    (OptionalGene(methodName, CycleObjectGene(methodName)))
                }
            }
            "null" ->
                return getReturnGene(
                        state,
                        tableType,
                        kindOfTableFieldType,
                        kindOfTableField,
                        tableFieldType,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
                        enumValues,
                        methodName,
                        unionTypes,
                        interfaceTypes,
                        accum,
                        maxNumberOfGenes,
                        tableFieldWithArgs
                )

            GqlConst.ENUM ->
                return createEnumGene(
                        kindOfTableField,
                        enumValues,
                )
            GqlConst.SCALAR ->
                return createScalarGene(
                        tableType,
                        kindOfTableField,
                )
            else ->
                return OptionalGene(tableType, StringGene(tableType))
        }
    }

    private fun createObjectGene(
            state: TempState,
            tableType: String,
            kindOfTableFieldType: String,
            /**
             * This history store the names of the object, union and interface types (i.e. tableFieldType in Table.kt ).
             * It is used in cycles managements (detecting cycles due to object, union and interface types).
             */
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            methodName: String,
            accum: Int,
            maxNumberOfGenes: Int
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        var accum = accum

        /*Look after each field (not tuple) and construct it recursively
          */

        val selection = state.tablesIndexedByName.get(tableType) ?: listOf()

        for (tableElement in selection) {
            /*
            Contains the elements of a tuple
             */
            val tupleElements: MutableList<Gene> = mutableListOf()

            val ktfType = tableElement.kindOfTableFieldType.toString()
            val ktf = tableElement.kindOfTableField.toString()

            /*
            The field is with arguments (it is a tuple):
             construct its arguments (n-1 elements) and;
             its last element (return)
             */
            if (tableElement.tableFieldWithArgs) {
                /*
             Construct field s arguments (the n-1 elements of the tuple) first
             */
                for (argElement in state.argsTables) {
                    if (argElement.typeName == tableElement.fieldName) {
                        if (argElement.kindOfTableFieldType == SCALAR || argElement.kindOfTableFieldType == ENUM) {
                            /*
                        array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                         */
                            val gene = getInputScalarListOrEnumListGene(
                                    state,
                                    argElement.tableFieldType,
                                    argElement.kindOfTableField.toString(),
                                    argElement.kindOfTableFieldType.toString(),
                                    argElement.typeName.toString(),
                                    history,
                                    argElement.isKindOfTableFieldTypeOptional,
                                    argElement.isKindOfTableFieldOptional,
                                    argElement.enumValues,
                                    argElement.fieldName,
                                    argElement.tableFieldWithArgs
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
                                    argElement.tableFieldType,
                                    argElement.kindOfTableField.toString(),
                                    argElement.kindOfTableFieldType.toString(),
                                    argElement.typeName.toString(),
                                    history,
                                    argElement.isKindOfTableFieldTypeOptional,
                                    argElement.isKindOfTableFieldOptional,
                                    argElement.enumValues,
                                    argElement.fieldName,
                                    argElement.unionTypes,
                                    argElement.interfaceTypes, maxNumberOfGenes,
                                    argElement.tableFieldWithArgs
                            )
                            tupleElements.add(gene)
                        }
                    }
                }
                /*
             Construct the last element (the return)
             */
                constructReturn(
                        ktfType,
                        state,
                        tableElement,
                        ktf,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
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
                        ktfType,
                        state,
                        tableElement,
                        ktf,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
                        accum,
                        maxNumberOfGenes,
                        fields
                )
        }

        return ObjectGene(methodName, fields, tableType)
    }

    private fun constructReturn(
            ktfType: String,
            state: TempState,
            tableElement: Table,
            ktf: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            accum: Int,
            maxNumberOfGenes: Int,
            tupleElements: MutableList<Gene>
    ) {

        if (ktf.lowercase() == GqlConst.LIST) {
            val gene =
                    getReturnGene(
                            state,
                            tableElement.tableFieldType,
                            ktf,
                            ktfType,
                            tableElement.tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            tableElement.enumValues,
                            tableElement.fieldName,
                            tableElement.unionTypes,
                            tableElement.interfaceTypes,
                            accum,
                            maxNumberOfGenes,
                            tableElement.tableFieldWithArgs
                    )
            tupleElements.add(gene)
        } else
            when (ktfType.lowercase()) {
                GqlConst.OBJECT -> {
                    val gene =
                            getReturnGene(
                                    state,
                                    tableElement.tableFieldType,
                                    ktfType,
                                    ktf,
                                    tableElement.tableFieldType,
                                    history,
                                    isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    tableElement.enumValues,
                                    tableElement.fieldName,
                                    tableElement.unionTypes,
                                    tableElement.interfaceTypes,
                                    accum,
                                    maxNumberOfGenes,
                                    tableElement.tableFieldWithArgs
                            )
                    tupleElements.add(gene)
                }
                GqlConst.SCALAR -> {
                    val gene = createScalarGene(
                            tableElement.tableFieldType,
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
                    val template =
                            getReturnGene(
                                    state,
                                    tableElement.tableFieldType,
                                    ktfType,
                                    ktf,
                                    tableElement.tableFieldType,
                                    history,
                                    isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    tableElement.enumValues,
                                    tableElement.fieldName,
                                    tableElement.unionTypes,
                                    tableElement.interfaceTypes,
                                    accum,
                                    maxNumberOfGenes,
                                    tableElement.tableFieldWithArgs
                            )
                    tupleElements.add(template)

                }
                GqlConst.INTERFACE -> {
                    val template =
                            getReturnGene(
                                    state,
                                    tableElement.tableFieldType,
                                    ktfType,
                                    ktf,
                                    tableElement.tableFieldType,
                                    history,
                                    isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    tableElement.enumValues,
                                    tableElement.fieldName,
                                    tableElement.unionTypes,
                                    tableElement.interfaceTypes,
                                    accum,
                                    maxNumberOfGenes,
                                    tableElement.tableFieldWithArgs
                            )
                    tupleElements.add(template)
                }
            }
    }

    /**
     * Create the correspondent object gene for each object defining the union type
     */
    private fun createUnionObjectsGene(
            state: TempState,
            tableType: String,
            kindOfTableFieldType: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            methodName: String,
            unionTypes: MutableList<String>,
            accum: Int,
            maxNumberOfGenes: Int
    ): Gene {

        val fields: MutableList<Gene> = mutableListOf()

        var accum = accum
        val initAccum =
                accum // needed since we restore the accumulator each time we construct one object defining the union

        for (elementInUnionTypes in unionTypes) {//Browse all objects defining the union
            accum += 1
            if (checkDepth(accum, maxNumberOfGenes)) {
                history.addLast(elementInUnionTypes)
                val objGeneTemplate = createObjectGene(
                        state, elementInUnionTypes, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, elementInUnionTypes, accum,
                        maxNumberOfGenes
                )

                history.removeLast()
                fields.add(OptionalGene(objGeneTemplate.name, objGeneTemplate))
            } else {
                fields.add(OptionalGene(elementInUnionTypes, LimitObjectGene(elementInUnionTypes)))
            }
            accum = initAccum
        }
        return ObjectGene(methodName + GqlConst.UNION_TAG, fields, tableType)
    }

    private fun createInterfaceObjectGene(
            state: TempState,
            kindOfTableFieldType: String,
            history: Deque<String>,
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean,
            interfaceTypes: MutableList<String>,
            interfaceBaseOptObjGene: Gene,
            accum: Int,
            maxNumberOfGenes: Int
    ): MutableList<Gene> {

        val fields: MutableList<Gene> = mutableListOf()

        var accum = accum
        val initAccum =
                accum // needed since we restore the accumulator each time we construct one object defining the interface

        for (elementInInterfaceTypes in interfaceTypes) {//Browse all additional objects in the interface
            accum += 1
            if (checkDepth(accum, maxNumberOfGenes)) {
                history.addLast(elementInInterfaceTypes)
                val objGeneTemplate = createObjectGene(
                        state, elementInInterfaceTypes, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, elementInInterfaceTypes, accum,
                        maxNumberOfGenes
                )
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
            enumValues: MutableList<String>,
    ): Gene {

        return OptionalGene(tableType, EnumGene(tableType, enumValues))

    }

}

