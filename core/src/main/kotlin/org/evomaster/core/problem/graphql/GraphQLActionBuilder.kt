package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.graphql.schema.FullType
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.problem.graphql.schema.__Field
import org.evomaster.core.problem.graphql.schema.__TypeKind
import org.evomaster.core.problem.graphql.schema.__TypeKind.*
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GraphQLActionBuilder {

    private val log: Logger = LoggerFactory.getLogger(GraphQLActionBuilder::class.java)
    private val idGenerator = AtomicInteger()

    private val systemTypes = listOf("__Schema", "__Directive", "__DirectiveLocation", "__EnumValue",
            "__Field", "__InputValue", "__Type", "__TypeKind")


    private data class TempState(
            val tables: MutableList<Table> = mutableListOf(),
            val argsTables: MutableList<Table> = mutableListOf(),
            val tempArgsTables: MutableList<Table> = mutableListOf()
    )

    /**
     * @param schema: the schema extracted from a GraphQL API, as a JSON string
     * @param actionCluster: for each mutation/query in the schema, populate this map with
     *                      new action templates.
     */
    fun addActionsFromSchema(schema: String, actionCluster: MutableMap<String, Action>) {

        val state = TempState()

        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(schema, SchemaObj::class.java)

        /*
         TODO
            - go through every Query and Mutation
            - create action for it which the needed genes
            - add the action to actionCluster
         */


        initTablesInfo(schemaObj, state)
        for (element in state.tables) {
            if (element.tableType == "Mutation" || element.tableType == "Query") {
                handleOperation(
                        state,
                        actionCluster,
                        element.tableField,
                        element.tableType,
                        element.tableFieldType,
                        element.kindOfTableFieldType.toString(),
                        element.kindOfTableField.toString(),
                        element.tableType.toString(),
                        element.isKindOfTableFieldTypeOptional,
                        element.isKindOfTableFieldOptional,
                        element.tableFieldWithArgs
                )
            }
        }

    }

    private fun initTablesInfo(schemaObj: SchemaObj, state: TempState) {


        for (elementIntypes in schemaObj.data?.__schema?.types) {
            if (systemTypes.contains(elementIntypes.name)) {
                continue
            }

            for (elementInfields in elementIntypes?.fields.orEmpty()) {

                /**
                 * extracting tables
                 */
                val tableElement = Table()
                tableElement.tableField = elementInfields?.name


                if (elementInfields?.type?.kind == NON_NULL) {// non optional list or object or scalar

                    handleNonNull(elementInfields, tableElement, elementIntypes, state)

                } else {
                    handleNull(elementInfields, tableElement, elementIntypes, state)
                }

                /**
                 * extracting argsTables: 1/2
                 */
                if (elementInfields.args.isNotEmpty()) {
                    tableElement.tableFieldWithArgs = true
                    for (elementInArgs in elementInfields.args) {
                        val inputElement = Table()
                        inputElement.tableType = elementInfields?.name
                        if (elementInArgs?.type?.kind == NON_NULL) {//non optional list or object or scalar
                            val list: __TypeKind? = __TypeKind.LIST
                            if (elementInArgs?.type?.ofType?.kind == list) {//non optional list
                                inputElement.kindOfTableField = list
                                inputElement.isKindOfTableFieldOptional = false
                                if (elementInArgs?.type?.ofType?.ofType?.kind == NON_NULL) {// non optional input object or scalar
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT
                                    if (elementInArgs?.type?.ofType?.ofType?.ofType?.kind == inputObject) {// non optional input object
                                        inputElement.kindOfTableFieldType = inputObject
                                        inputElement.isKindOfTableFieldTypeOptional = false
                                        inputElement.tableFieldType = elementInArgs?.type?.ofType?.ofType?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        state.argsTables.add(inputElement)
                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR// non optional scalar
                                        if (elementInArgs?.type?.ofType?.ofType?.ofType?.kind == scalar) {
                                            inputElement.kindOfTableFieldType = scalar
                                            inputElement.isKindOfTableFieldTypeOptional = false
                                            inputElement.tableFieldType = elementInArgs?.type?.ofType?.ofType?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            state.argsTables.add(inputElement)
                                        }
                                    }
                                } else {
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT // optional input object
                                    if (elementInArgs?.type?.ofType?.ofType?.kind == inputObject) {
                                        inputElement.kindOfTableFieldType = inputObject
                                        inputElement.isKindOfTableFieldTypeOptional = true
                                        inputElement.tableFieldType = elementInArgs?.type?.ofType?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        state.argsTables.add(inputElement)

                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR //optional scalar
                                        if (elementInArgs?.type?.ofType?.ofType?.kind == scalar) {
                                            inputElement.kindOfTableFieldType = scalar
                                            inputElement.isKindOfTableFieldTypeOptional = true
                                            inputElement.tableFieldType = elementInArgs?.type?.ofType?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            state.argsTables.add(inputElement)
                                        }

                                    }

                                }
                            } else {
                                val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT // non optional input object not in a list
                                if (elementInArgs?.type?.ofType?.kind == inputObject) {
                                    inputElement.kindOfTableFieldType = inputObject
                                    inputElement.isKindOfTableFieldTypeOptional = false
                                    inputElement.tableFieldType = elementInArgs?.type?.ofType?.name
                                    inputElement.tableField = elementInArgs?.name
                                    state.argsTables.add(inputElement)
                                } else {
                                    val scalar: __TypeKind? = __TypeKind.SCALAR //non optional scalar not in a list
                                    if (elementInArgs?.type?.ofType?.kind == scalar) {
                                        inputElement.kindOfTableFieldType = scalar
                                        inputElement.isKindOfTableFieldTypeOptional = false
                                        inputElement.tableFieldType = elementInArgs?.type?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        state.argsTables.add(inputElement)
                                    }
                                }
                            }
                        } else {
                            val list: __TypeKind? = __TypeKind.LIST//optional list or input object or scalar
                            if (elementInArgs?.type?.kind == list) {//optional list in the top
                                inputElement.kindOfTableField = list
                                inputElement.isKindOfTableFieldOptional = true
                                if (elementInArgs?.type?.ofType.kind == NON_NULL) {// non optional input object or scalar
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT
                                    if (elementInArgs?.type?.ofType?.ofType?.kind == inputObject) {// non optional input object
                                        inputElement.kindOfTableFieldType = inputObject
                                        inputElement.isKindOfTableFieldTypeOptional = false
                                        inputElement.tableFieldType = elementInArgs?.type?.ofType?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        state.argsTables.add(inputElement)
                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR
                                        if (elementInArgs?.type?.ofType?.ofType?.kind == scalar) {// non optional scalar
                                            inputElement.kindOfTableFieldType = scalar
                                            inputElement.isKindOfTableFieldTypeOptional = false
                                            inputElement.tableFieldType = elementInArgs?.type?.ofType?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            state.argsTables.add(inputElement)
                                        }
                                    }

                                } else {
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT//optional input object or scalar
                                    if (elementInArgs?.type?.ofType.kind == inputObject) {//optional input object
                                        inputElement.kindOfTableFieldType = inputObject
                                        inputElement.isKindOfTableFieldTypeOptional = true
                                        inputElement.tableFieldType = elementInArgs?.type?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        state.argsTables.add(inputElement)
                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR
                                        if (elementInArgs?.type?.ofType?.kind == scalar) {// optional scalar
                                            inputElement.kindOfTableFieldType = scalar
                                            inputElement.isKindOfTableFieldTypeOptional = true
                                            inputElement.tableFieldType = elementInArgs?.type?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            state.argsTables.add(inputElement)
                                        }
                                    }
                                }

                            } else {
                                val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT // optional input object in the top
                                if (elementInArgs?.type?.kind == inputObject) {
                                    inputElement.kindOfTableFieldType = inputObject
                                    inputElement.isKindOfTableFieldTypeOptional = true
                                    inputElement.tableFieldType = elementInArgs?.type?.name
                                    inputElement.tableField = elementInArgs?.name
                                    state.argsTables.add(inputElement)
                                } else {
                                    val scalar: __TypeKind? = __TypeKind.SCALAR// optional scalar in the top
                                    if (elementInArgs?.type?.kind == scalar) {
                                        inputElement.kindOfTableFieldType = scalar
                                        inputElement.isKindOfTableFieldTypeOptional = true
                                        inputElement.tableFieldType = elementInArgs?.type?.name
                                        inputElement.tableField = elementInArgs?.name
                                        state.argsTables.add(inputElement)
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
        /**
         *extracting tempArgsTables, an intermediate table for extracting argsTables
         */
        for (elementInInputParamTable in state.argsTables) {
            val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT
            if (elementInInputParamTable.kindOfTableFieldType == inputObject) {
                for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {
                    if (elementInInputParamTable.tableFieldType == elementIntypes.name) {
                        if (elementIntypes.kind == inputObject) {
                            for (elementInInputFields in elementIntypes.inputFields) {
                                val non_null: __TypeKind? = __TypeKind.NON_NULL
                                if (elementInInputFields?.type?.kind == non_null) {//non optional scalar or enum //TODO enum
                                    val scalar: __TypeKind? = __TypeKind.SCALAR
                                    if (elementInInputFields?.type?.ofType?.kind == scalar) {// non optional scalar
                                        val inputElement = Table()
                                        inputElement.tableType = elementIntypes.name
                                        inputElement.kindOfTableFieldType = scalar
                                        inputElement.isKindOfTableFieldTypeOptional = false
                                        inputElement.tableFieldType = elementInInputFields?.type?.ofType?.name
                                        inputElement.tableField = elementInInputFields?.name
                                        state.tempArgsTables.add(inputElement)
                                    }
                                } else {
                                    val scalar: __TypeKind? = __TypeKind.SCALAR// optional scalar or enum
                                    if (elementInInputFields?.type?.kind == scalar) {// optional scalar
                                        val inputElement = Table()
                                        inputElement.tableType = elementIntypes.name
                                        inputElement.kindOfTableFieldType = scalar
                                        inputElement.isKindOfTableFieldTypeOptional = true
                                        inputElement.tableFieldType = elementInInputFields?.type?.name
                                        inputElement.tableField = elementInInputFields?.name
                                        state.tempArgsTables.add(inputElement)
                                    }
                                }
                            }

                        }
                    }

                }

            }
        }

        /**
         * merging argsTables with tempArgsTables: extracting argsTables: 2/2
         */
        state.argsTables.addAll(state.tempArgsTables)
    }

    /*
        This when an entry is optional
     */
    private fun handleNull(elementInfields: __Field, tableElement: Table, elementIntypes: FullType, state: TempState) {

        val list: __TypeKind? = LIST//optional list or object or scalar

        if (elementInfields?.type?.kind == list) {//optional list in the top
            tableElement.kindOfTableField = list
            tableElement.isKindOfTableFieldOptional = true
            if (elementInfields?.type?.ofType.kind == NON_NULL) {// non optional object or scalar
                val obj: __TypeKind? = OBJECT
                if (elementInfields?.type?.ofType?.ofType?.kind == obj) {// non optional object
                    tableElement.kindOfTableFieldType = obj
                    tableElement.isKindOfTableFieldTypeOptional = false
                    tableElement.tableFieldType = elementInfields?.type?.ofType?.ofType?.name
                    tableElement.tableType = elementIntypes?.name
                    state.tables.add(tableElement)
                } else {
                    val scalar: __TypeKind? = SCALAR
                    if (elementInfields?.type?.ofType?.ofType?.kind == scalar) {// non optional scalar
                        tableElement.kindOfTableFieldType = scalar
                        tableElement.isKindOfTableFieldTypeOptional = false
                        tableElement.tableFieldType = elementInfields?.type?.ofType?.ofType?.name
                        tableElement.tableType = elementIntypes?.name
                        state.tables.add(tableElement)
                    }
                }

            } else {
                val obj: __TypeKind? = OBJECT //optional object or scalar
                if (elementInfields?.type?.ofType.kind == obj) {//optional object
                    tableElement.kindOfTableFieldType = obj
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields?.type?.ofType?.name
                    tableElement.tableType = elementIntypes?.name
                    state.tables.add(tableElement)
                } else {
                    val scalar: __TypeKind? = SCALAR
                    if (elementInfields?.type?.ofType?.kind == scalar) {// optional scalar
                        tableElement.kindOfTableFieldType = scalar
                        tableElement.isKindOfTableFieldTypeOptional = true
                        tableElement.tableFieldType = elementInfields?.type?.ofType?.name
                        tableElement.tableType = elementIntypes?.name
                        state.tables.add(tableElement)
                    }
                }
            }

        } else {
            val obj: __TypeKind? = OBJECT
            if (elementInfields?.type?.kind == obj) {// optional object in the top
                tableElement.kindOfTableFieldType = obj
                tableElement.isKindOfTableFieldTypeOptional = true
                tableElement.tableFieldType = elementInfields?.type?.name
                tableElement.tableType = elementIntypes?.name
                state.tables.add(tableElement)
            } else {
                val scalar: __TypeKind? = SCALAR
                if (elementInfields?.type?.kind == scalar) {/// optional scalar in the top
                    tableElement.kindOfTableFieldType = scalar
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields?.type?.name
                    tableElement.tableType = elementIntypes?.name
                    state.tables.add(tableElement)
                }
            }
        }
    }

    /*
        this is to handle entries that are NOT optional, and must be there, ie, they cannot be null
     */
    private fun handleNonNull(elementInfields: __Field, tableElement: Table, elementIntypes: FullType, state: TempState) {

        val kind = elementInfields?.type?.ofType?.kind
        val kind2 = elementInfields?.type?.ofType?.ofType?.kind
        val kind3 = elementInfields?.type?.ofType?.ofType?.ofType?.kind
        tableElement.isKindOfTableFieldOptional = false

        if (kind == LIST) {// non optional list
            tableElement.kindOfTableField = LIST

            if (kind2 == NON_NULL) {// non optional object or scalar
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = kind3
                tableElement.tableFieldType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                tableElement.tableType = elementIntypes?.name
                state.tables.add(tableElement)
            } else {
                tableElement.kindOfTableFieldType = kind2
                tableElement.isKindOfTableFieldTypeOptional = true
                tableElement.tableFieldType = elementInfields?.type?.ofType?.ofType?.name
                tableElement.tableType = elementIntypes?.name
                state.tables.add(tableElement)
            }
        } else if (kind == OBJECT || kind == SCALAR) {
            tableElement.kindOfTableFieldType = kind
            tableElement.tableFieldType = elementInfields?.type?.ofType?.name
            tableElement.tableType = elementIntypes?.name
            state.tables.add(tableElement)
        }  else {
            throw IllegalArgumentException("Type not supported yet: " + elementInfields?.type?.ofType?.kind)
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
            tableFieldWithArgs: Boolean
    ) {
        if (methodName == null) {
            //TODO log warn
            return;
        }
        if (methodType == null) {
            //TODO log warn
            return;
        }
        val type = when {
            methodType.equals("QUERY", true) -> GQMethodType.QUERY
            methodType.equals("MUTATION", true) -> GQMethodType.MUTATION
            else -> {
                //TODO log warn
                return
            }
        }

        val actionId = "$methodName${idGenerator.incrementAndGet()}"

        val params = extractParams(state, methodName, tableFieldType, kindOfTableFieldType, kindOfTableField,
                tableType, isKindOfTableFieldTypeOptional,
                isKindOfTableFieldOptional, tableFieldWithArgs)

        val action = GraphQLAction(actionId, methodName, type, params)

        actionCluster[action.getName()] = action

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
            tableFieldWithArgs: Boolean

    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque<String>()

        if (tableFieldWithArgs) {

            for (element in state.argsTables) {

                if (element.tableType == methodName) {

                    val gene = getGene(state, element.tableFieldType, element.kindOfTableField.toString(), element.kindOfTableFieldType.toString(), element.tableType.toString(), history,
                            element.isKindOfTableFieldTypeOptional, element.isKindOfTableFieldOptional)

                    params.add(GQInputParam(element.tableFieldType, gene))
                }
            }
            history.addFirst(tableFieldType)
            val gene = getGene(state, tableFieldType, kindOfTableField, kindOfTableFieldType, tableType, history,
                    isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)

            params.add(GQReturnParam(tableFieldType, gene))
        } else {
            history.addFirst(tableFieldType)
            val gene = getGene(state, tableFieldType, kindOfTableField, kindOfTableFieldType, tableType, history,
                    isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)

            params.add(GQReturnParam(tableFieldType, gene))

        }

        return params
    }

    private fun getGene(
            state: TempState,
            tableFieldType: String,
            kindOfTableField: String?,
            kindOfTableFieldType: String,
            tableType: String,
            history: Deque<String> = ArrayDeque<String>(),
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean
    ): Gene {
        when (kindOfTableField?.toLowerCase()) {

            "list" -> {

                if (isKindOfTableFieldOptional) {
                    val template = getGene(state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                    return OptionalGene(tableType, ArrayGene(tableType, template))

                } else {
                    val template = getGene(state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                    return ArrayGene(tableType, template)
                }
            }
            "object" -> {
                if (isKindOfTableFieldTypeOptional) {
                    val optObjGene = createObjectGene(state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                    return OptionalGene(tableType, optObjGene)
                } else {
                    return createObjectGene(state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                }
            }
            "input_object" -> {
                if (isKindOfTableFieldTypeOptional) {
                    val optInputObjGene = createInputObjectGene(state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                    return OptionalGene(tableType, optInputObjGene)
                } else {
                    return createInputObjectGene(state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                }
            }

            "int" -> {
                if (isKindOfTableFieldTypeOptional) {
                    return OptionalGene(tableType, IntegerGene(tableType))
                } else {
                    return IntegerGene(tableType)
                }
            }
            "string" -> {
                if (isKindOfTableFieldTypeOptional) {
                    return OptionalGene(tableType, StringGene(tableType))
                } else {
                    return StringGene(tableType)
                }
            }
            "float" -> {
                if (isKindOfTableFieldTypeOptional) {
                    return OptionalGene(tableType, FloatGene(tableType))
                } else {
                    return FloatGene(tableType)
                }
            }

            "boolean" -> {
                if (isKindOfTableFieldTypeOptional) {
                    return OptionalGene(tableType, BooleanGene(tableType))
                } else {
                    return BooleanGene(tableType)
                }
            }
            "null" -> {
                return getGene(state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
            }
            "date" -> {
                if (isKindOfTableFieldTypeOptional) {
                    return OptionalGene(tableType, BooleanGene(tableType))
                } else {
                    return DateGene(tableType)
                }
            }
            "scalar" -> {
                if (isKindOfTableFieldTypeOptional) {
                    return OptionalGene(tableType, getGene(state, tableFieldType, tableType, kindOfTableField, tableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional))
                } else {

                    return getGene(state, tableFieldType, tableType, kindOfTableFieldType, kindOfTableField, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                }
            }

            else -> {
                LoggingUtil.uniqueWarn(log, "Kind Of Table Field not supported yet: $kindOfTableField")
                return StringGene("TODO")
            }
        }
    }

    private fun createObjectGene(
            state: TempState,
            tableType: String,
            kindOfTableFieldType: String,
            history: Deque<String> = ArrayDeque<String>(),
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean
    ): Gene {

        val fields: MutableList<Gene> = mutableListOf()
        for (element in state.tables) {
            if (element.tableType == tableType) {
                if (element.kindOfTableFieldType.toString().equals("SCALAR", ignoreCase = true)) {
                    val field = element.tableField
                    val template = field?.let {
                        getGene(state, tableType, element.tableFieldType, kindOfTableFieldType, it, history,
                                element.isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                    }
                    if (template != null) {
                        fields.add(template)
                    }
                } else {
                    if (element.kindOfTableField.toString().equals("LIST", ignoreCase = true)) {
                        val template = getGene(state, element.tableFieldType, element.kindOfTableField.toString(), element.kindOfTableFieldType.toString(),
                                element.tableFieldType, history, isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)

                        if (template != null) {
                            fields.add(template)
                        }
                    } else {
                        if (element.kindOfTableFieldType.toString().equals("OBJECT", ignoreCase = true)) {
                            history.addLast(element.tableType)
                            history.addLast(element.tableFieldType)
                            if (history.count { it == element.tableFieldType } == 1) {
                                val template = getGene(state, element.tableFieldType, element.kindOfTableFieldType.toString(), element.kindOfTableField.toString(),
                                        element.tableFieldType, history, isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                                history.removeLast()
                                if (template != null) {
                                    fields.add(template)
                                }
                            } else {
                                fields.add(CycleObjectGene(element.tableFieldType))
                                history.removeLast()

                            }
                        }
                    }
                }
            }

        }
        return ObjectGene(tableType, fields, tableType)
    }

    private fun createInputObjectGene(
            state: TempState,
            tableType: String,
            kindOfTableFieldType: String,
            history: Deque<String> = ArrayDeque<String>(),
            isKindOfTableFieldTypeOptional: Boolean,
            isKindOfTableFieldOptional: Boolean
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        for (element in state.argsTables) {
            if (element.tableType == tableType) {
                if (element.kindOfTableFieldType.toString().equals("SCALAR", ignoreCase = true)) {
                    val field = element.tableField
                    val template = field?.let {
                        getGene(state, tableType, element.tableFieldType, kindOfTableFieldType, it, history,
                                element.isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                    }
                    if (template != null) {
                        fields.add(template)
                    }
                } else {
                    if (element.kindOfTableField.toString().equals("LIST", ignoreCase = true)) {
                        val template = getGene(state, element.tableFieldType, element.kindOfTableField.toString(),
                                element.kindOfTableFieldType.toString(),
                                element.tableFieldType, history, isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)

                        if (template != null) {
                            fields.add(template)
                        }
                    } else {
                        if (element.kindOfTableFieldType.toString().equals("INPUT_OBJECT", ignoreCase = true)) {
                            val template = getGene(state, element.tableFieldType, element.kindOfTableFieldType.toString(), element.kindOfTableField.toString(),
                                    element.tableFieldType, history, isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional)
                            if (template != null) {
                                fields.add(template)
                            }
                        }
                    }
                }
            }

        }
        return ObjectGene(tableType, fields, tableType)
    }

}