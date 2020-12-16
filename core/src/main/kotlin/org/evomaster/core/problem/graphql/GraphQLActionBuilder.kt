package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.problem.graphql.schema.__TypeKind
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
    var tables: MutableList<Table> = mutableListOf()
    var argsTables: MutableList<Table> = mutableListOf()
    var tempArgsTables: MutableList<Table> = mutableListOf()

    /**
     * @param schema: the schema extracted from a GraphQL API, as a JSON string
     * @param actionCluster: for each mutation/query in the schema, populate this map with
     *                      new action templates.
     */

    fun addActionsFromSchema(schema: String, actionCluster: MutableMap<String, Action>) {

        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(schema, SchemaObj::class.java)

        /*
         TODO
            - go through every Query and Mutation
            - create action for it which the needed genes
            - add the action to actionCluster
         */


        initTablesInfo(schemaObj)
        for (element in tables) {
            if (element.tableName == "Mutation" || element.tableName == "Query") {
                handleOperation(actionCluster,
                        element.tableField,
                        element.tableName,
                        element.tableType,
                        element.kindOfTableType.toString(),
                        element.kindOfTableField.toString(),
                        element.tableName.toString(),
                        element.isKindOfTableTypeOptional,
                        element.isKindOfKindOfTableFieldOptional,
                        element.fieldWithArgs
                )
            }
        }

    }

    private fun initTablesInfo(schemaObj: SchemaObj) {
        for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {
            if (elementIntypes.name == "__Schema" ||
                    elementIntypes.name == "__Directive" ||
                    elementIntypes.name == "__DirectiveLocation" ||
                    elementIntypes.name == "__EnumValue" ||
                    elementIntypes.name == "__Field" ||
                    elementIntypes.name == "__InputValue" ||
                    elementIntypes.name == "__Type" ||
                    elementIntypes.name == "__TypeKind"

            ) {
                break
            }
            for (elementInfields in elementIntypes?.fields.orEmpty()) {

                /**
                 * extracting tables
                 */
                val tableElement = Table()
                tableElement.tableField = elementInfields?.name
                val non_null: __TypeKind? = __TypeKind.NON_NULL
                if (elementInfields?.type?.kind == non_null) {// non optional list or object or scalar
                    val list: __TypeKind? = __TypeKind.LIST
                    if (elementInfields?.type?.ofType?.kind == list) {// non optional list
                        tableElement.kindOfTableField = list
                        tableElement.isKindOfKindOfTableFieldOptional = false
                        if (elementInfields?.type?.ofType?.ofType?.kind == non_null) {// non optional object or scalar
                            val obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType?.ofType?.ofType?.kind == obj) {// non optional object
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            } else {
                                val scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.ofType?.ofType?.kind == scalar) {// non optional scalar
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = false
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }
                            }

                        } else {
                            val obj: __TypeKind? = __TypeKind.OBJECT // optional object
                            if (elementInfields?.type?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)

                            } else {
                                val scalar: __TypeKind? = __TypeKind.SCALAR //optional scalar
                                if (elementInfields?.type?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = true
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }
                            }
                        }
                    } else {
                        val obj: __TypeKind? = __TypeKind.OBJECT // object non optional not in a list
                        if (elementInfields?.type?.ofType?.kind == obj) {
                            tableElement.kindOfTableType = obj
                            tableElement.isKindOfTableTypeOptional = false
                            tableElement.tableType = elementInfields?.type?.ofType?.name
                            tableElement.tableName = elementIntypes?.name
                            tables.add(tableElement)
                        } else {
                            val scalar: __TypeKind? = __TypeKind.SCALAR
                            if (elementInfields?.type?.ofType?.kind == scalar) {
                                tableElement.kindOfTableType = scalar
                                tableElement.isKindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            }

                        }

                    }

                } else {
                    val list: __TypeKind? = __TypeKind.LIST//optional list or object or scalar
                    if (elementInfields?.type?.kind == list) {//optional list in the top
                        tableElement.kindOfTableField = list
                        tableElement.isKindOfKindOfTableFieldOptional = true
                        if (elementInfields?.type?.ofType.kind == non_null) {// non optional object or scalar
                            val obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType?.ofType?.kind == obj) {// non optional object
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            } else {
                                val scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.ofType?.kind == scalar) {// non optional scalar
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = false
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }
                            }

                        } else {
                            val obj: __TypeKind? = __TypeKind.OBJECT //optional object or scalar
                            if (elementInfields?.type?.ofType.kind == obj) {//optional object
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            } else {
                                val scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.kind == scalar) {// optional scalar
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = true
                                    tableElement.tableType = elementInfields?.type?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }
                            }
                        }

                    } else {
                        val obj: __TypeKind? = __TypeKind.OBJECT
                        if (elementInfields?.type?.kind == obj) {// optional object in the top
                            tableElement.kindOfTableType = obj
                            tableElement.isKindOfTableTypeOptional = true
                            tableElement.tableType = elementInfields?.type?.name
                            tableElement.tableName = elementIntypes?.name
                            tables.add(tableElement)
                        } else {
                            val scalar: __TypeKind? = __TypeKind.SCALAR
                            if (elementInfields?.type?.kind == scalar) {/// optional scalar in the top
                                tableElement.kindOfTableType = scalar
                                tableElement.isKindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            }
                        }
                    }
                }

                /**
                 * extracting argsTables: 1/2
                 */
                if (elementInfields.args.isNotEmpty()) {
                    tableElement.fieldWithArgs = true
                    for (elementInArgs in elementInfields.args) {
                        val inputElement = Table()
                        inputElement.tableName = elementInfields?.name
                        if (elementInArgs?.type?.kind == non_null) {//non optional list or object or scalar
                            val list: __TypeKind? = __TypeKind.LIST
                            if (elementInArgs?.type?.ofType?.kind == list) {//non optional list
                                inputElement.kindOfTableField = list
                                inputElement.isKindOfKindOfTableFieldOptional = false
                                if (elementInArgs?.type?.ofType?.ofType?.kind == non_null) {// non optional input object or scalar
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT
                                    if (elementInArgs?.type?.ofType?.ofType?.ofType?.kind == inputObject) {// non optional input object
                                        inputElement.kindOfTableType = inputObject
                                        inputElement.isKindOfTableTypeOptional = false
                                        inputElement.tableType = elementInArgs?.type?.ofType?.ofType?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        argsTables.add(inputElement)
                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR// non optional scalar
                                        if (elementInArgs?.type?.ofType?.ofType?.ofType?.kind == scalar) {
                                            inputElement.kindOfTableType = scalar
                                            inputElement.isKindOfTableTypeOptional = false
                                            inputElement.tableType = elementInArgs?.type?.ofType?.ofType?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            argsTables.add(inputElement)
                                        }
                                    }
                                } else {
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT // optional input object
                                    if (elementInArgs?.type?.ofType?.ofType?.kind == inputObject) {
                                        inputElement.kindOfTableType = inputObject
                                        inputElement.isKindOfTableTypeOptional = true
                                        inputElement.tableType = elementInArgs?.type?.ofType?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        argsTables.add(inputElement)

                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR //optional scalar
                                        if (elementInArgs?.type?.ofType?.ofType?.kind == scalar) {
                                            inputElement.kindOfTableType = scalar
                                            inputElement.isKindOfTableTypeOptional = true
                                            inputElement.tableType = elementInArgs?.type?.ofType?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            argsTables.add(inputElement)
                                        }

                                    }

                                }
                            } else {
                                val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT // non optional input object not in a list
                                if (elementInArgs?.type?.ofType?.kind == inputObject) {
                                    inputElement.kindOfTableType = inputObject
                                    inputElement.isKindOfTableTypeOptional = false
                                    inputElement.tableType = elementInArgs?.type?.ofType?.name
                                    inputElement.tableField = elementInArgs?.name
                                    argsTables.add(inputElement)
                                } else {
                                    val scalar: __TypeKind? = __TypeKind.SCALAR //non optional scalar not in a list
                                    if (elementInArgs?.type?.ofType?.kind == scalar) {
                                        inputElement.kindOfTableType = scalar
                                        inputElement.isKindOfTableTypeOptional = false
                                        inputElement.tableType = elementInArgs?.type?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        argsTables.add(inputElement)
                                    }
                                }
                            }
                        } else {
                            val list: __TypeKind? = __TypeKind.LIST//optional list or input object or scalar
                            if (elementInArgs?.type?.kind == list) {//optional list in the top
                                inputElement.kindOfTableField = list
                                inputElement.isKindOfKindOfTableFieldOptional = true
                                if (elementInArgs?.type?.ofType.kind == non_null) {// non optional input object or scalar
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT
                                    if (elementInArgs?.type?.ofType?.ofType?.kind == inputObject) {// non optional input object
                                        inputElement.kindOfTableType = inputObject
                                        inputElement.isKindOfTableTypeOptional = false
                                        inputElement.tableType = elementInArgs?.type?.ofType?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        argsTables.add(inputElement)
                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR
                                        if (elementInArgs?.type?.ofType?.ofType?.kind == scalar) {// non optional scalar
                                            inputElement.kindOfTableType = scalar
                                            inputElement.isKindOfTableTypeOptional = false
                                            inputElement.tableType = elementInArgs?.type?.ofType?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            argsTables.add(inputElement)
                                        }
                                    }

                                } else {
                                    val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT//optional input object or scalar
                                    if (elementInArgs?.type?.ofType.kind == inputObject) {//optional input object
                                        inputElement.kindOfTableType = inputObject
                                        inputElement.isKindOfTableTypeOptional = true
                                        inputElement.tableType = elementInArgs?.type?.ofType?.name
                                        inputElement.tableField = elementInArgs?.name
                                        argsTables.add(inputElement)
                                    } else {
                                        val scalar: __TypeKind? = __TypeKind.SCALAR
                                        if (elementInArgs?.type?.ofType?.kind == scalar) {// optional scalar
                                            inputElement.kindOfTableType = scalar
                                            inputElement.isKindOfTableTypeOptional = true
                                            inputElement.tableType = elementInArgs?.type?.ofType?.name
                                            inputElement.tableField = elementInArgs?.name
                                            argsTables.add(inputElement)
                                        }
                                    }
                                }

                            } else {
                                val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT // optional input object in the top
                                if (elementInArgs?.type?.kind == inputObject) {
                                    inputElement.kindOfTableType = inputObject
                                    inputElement.isKindOfTableTypeOptional = true
                                    inputElement.tableType = elementInArgs?.type?.name
                                    inputElement.tableField = elementInArgs?.name
                                    argsTables.add(inputElement)
                                } else {
                                    val scalar: __TypeKind? = __TypeKind.SCALAR// optional scalar in the top
                                    if (elementInArgs?.type?.kind == scalar) {
                                        inputElement.kindOfTableType = scalar
                                        inputElement.isKindOfTableTypeOptional = true
                                        inputElement.tableType = elementInArgs?.type?.name
                                        inputElement.tableField = elementInArgs?.name
                                        argsTables.add(inputElement)
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
        for (elementInInputParamTable in argsTables) {
            val inputObject: __TypeKind? = __TypeKind.INPUT_OBJECT
            if (elementInInputParamTable.kindOfTableType == inputObject) {
                for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {
                    if (elementInInputParamTable.tableType == elementIntypes.name) {
                        if (elementIntypes.kind == inputObject) {
                            for (elementInInputFields in elementIntypes.inputFields) {
                                val non_null: __TypeKind? = __TypeKind.NON_NULL
                                if (elementInInputFields?.type?.kind == non_null) {//non optional scalar or enum (not yet enum)
                                    val scalar: __TypeKind? = __TypeKind.SCALAR
                                    if (elementInInputFields?.type?.ofType?.kind == scalar) {// non optional scalar
                                        val inputElement = Table()
                                        inputElement.tableName = elementIntypes.name
                                        inputElement.kindOfTableType = scalar
                                        inputElement.isKindOfTableTypeOptional = false
                                        inputElement.tableType = elementInInputFields?.type?.ofType?.name
                                        inputElement.tableField = elementInInputFields?.name
                                        tempArgsTables.add(inputElement)
                                    }
                                } else {
                                    val scalar: __TypeKind? = __TypeKind.SCALAR// optional scalar or enum (not yet enum)
                                    if (elementInInputFields?.type?.kind == scalar) {// optional scalar
                                        val inputElement = Table()
                                        inputElement.tableName = elementIntypes.name
                                        inputElement.kindOfTableType = scalar
                                        inputElement.isKindOfTableTypeOptional = true
                                        inputElement.tableType = elementInInputFields?.type?.name
                                        inputElement.tableField = elementInInputFields?.name
                                        tempArgsTables.add(inputElement)
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
        argsTables.addAll(tempArgsTables)
    }

    private fun handleOperation(
            actionCluster: MutableMap<String, Action>,
            methodName: String?,
            methodType: String?,
            tableType: String,
            kindOfTableType: String,
            kindOfTableField: String?,
            tableName: String,
            isKindOfTableTypeOptional: Boolean,
            isKindOfkindOfTableFieldOptional: Boolean,
            fieldWithArgs: Boolean
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

        //TODO populate

        val params = extractParams(methodName, tableType, kindOfTableType, kindOfTableField,
                tableName, isKindOfTableTypeOptional,
                isKindOfkindOfTableFieldOptional, fieldWithArgs)

        val action = GraphQLAction(actionId, methodName, type, params)

        actionCluster[action.getName()] = action

    }

    private fun extractParams(
            methodName: String,
            tableType: String,
            kindOfTableType: String,
            kindOfTableField: String?,
            tableName: String,
            isKindOfTableTypeOptional: Boolean,
            isKindOfKindOfTableFieldOptional: Boolean,
            fieldWithArgs: Boolean

    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque<String>()

        if (fieldWithArgs) {

            for (element in argsTables) {

                if (element.tableName == methodName) {

                    val gene = getGene(element.tableType, element.kindOfTableField.toString(), element.kindOfTableType.toString(), element.tableName.toString(), history,
                            element.isKindOfTableTypeOptional, element.isKindOfKindOfTableFieldOptional)
                    params.add(GQInputParam(element.tableType, gene))
                }
            }
            val gene = getGene(tableType, kindOfTableField, kindOfTableType, tableName, history,
                    isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)

            params.add(GQReturnParam(tableType, gene))
        } else {
            val gene = getGene(tableType, kindOfTableField, kindOfTableType, tableName, history,
                    isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)

            params.add(GQReturnParam(tableType, gene))

        }

        return params
    }

    private fun getGene(
            tableType: String,
            kindOfTableField: String?,
            kindOfTableType: String,
            tableName: String,
            history: Deque<String> = ArrayDeque<String>(),
            isKindOfTableTypeOptional: Boolean,
            isKindOfKindOfTableFieldOptional: Boolean
    ): Gene {
        when (kindOfTableField?.toLowerCase()) {

            "list" -> {

                if (isKindOfKindOfTableFieldOptional) {
                    val template = getGene(tableName, kindOfTableType, kindOfTableField, tableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                    return OptionalGene(tableName, ArrayGene(tableName, template))

                } else {
                    val template = getGene(tableName, kindOfTableType, kindOfTableField, tableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                    return ArrayGene(tableName, template)
                }
            }
            "object" -> {
                if (isKindOfTableTypeOptional) {
                    val optObjGene = createObjectGene(tableName, tableType, kindOfTableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                    return OptionalGene(tableName, optObjGene)
                } else {
                    return createObjectGene(tableName, tableType, kindOfTableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                }
            }
            "input_object" -> {
                if (isKindOfTableTypeOptional) {
                    val optInputObjGene = createInputObjectGene(tableName, tableType, kindOfTableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                    return OptionalGene(tableName, optInputObjGene)
                } else {
                    return createInputObjectGene(tableName, tableType, kindOfTableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                }
            }

            "int" -> {
                if (isKindOfTableTypeOptional) {
                    return OptionalGene(tableName, IntegerGene(tableName))
                } else {
                    return IntegerGene(tableName)
                }
            }
            "string" -> {
                if (isKindOfTableTypeOptional) {
                    return OptionalGene(tableName, StringGene(tableName))
                } else {
                    return StringGene(tableName)
                }
            }
            "float" -> {
                if (isKindOfTableTypeOptional) {
                    return OptionalGene(tableName, FloatGene(tableName))
                } else {
                    return FloatGene(tableName)
                }
            }

            "boolean" -> {
                if (isKindOfTableTypeOptional) {
                    return OptionalGene(tableName, BooleanGene(tableName))
                } else {
                    return BooleanGene(tableName)
                }
            }
            "null" -> {
                return getGene(tableName, kindOfTableType, kindOfTableField, tableType, history,
                        isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
            }
            "date" -> {
                if (isKindOfTableTypeOptional) {
                    return OptionalGene(tableName, BooleanGene(tableName))
                } else {
                    return DateGene(tableName)
                }
            }
            "scalar" -> {
                if (isKindOfTableTypeOptional) {
                    return OptionalGene(tableName, getGene(tableType, tableName, kindOfTableField, tableType, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional))
                } else {

                    return getGene(tableType, tableName, kindOfTableType, kindOfTableField, history,
                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                }
            }


            else -> {
                LoggingUtil.uniqueWarn(log, "Kind Of Table Field not supported yet: $kindOfTableField")
                return StringGene("TODO")
            }
        }
    }

    private fun createObjectGene(tableName: String,
                                 tableType: String,
                                 kindOfTableType: String,
                                 history: Deque<String> = ArrayDeque<String>(),
                                 isKindOfTableTypeOptional: Boolean,
                                 isKindOfKindOfTableFieldOptional: Boolean
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        for (element in tables) {
            if (element.tableName == tableName) {
                if (element.kindOfTableType.toString().equals("SCALAR", ignoreCase = true)) {
                    val field = element.tableField
                    val template = field?.let {
                        getGene(tableName, element.tableType, kindOfTableType, it, history,
                                element.isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                    }
                    if (template != null) {
                        fields.add(template)
                    }
                } else {
                    if (element.kindOfTableField.toString().equals("LIST", ignoreCase = true)) {
                        val template = getGene(element.tableType, element.kindOfTableField.toString(), element.kindOfTableType.toString(),
                                element.tableType, history, isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)

                        if (template != null) {
                            fields.add(template)
                        }
                    } else {
                        if (element.kindOfTableType.toString().equals("OBJECT", ignoreCase = true)) {
                            history.add(element.tableName)
                            if (history.count { it == element.tableName } == 1) {
                                val template = getGene(element.tableType, element.kindOfTableType.toString(), element.kindOfTableField.toString(),
                                        element.tableType, history, isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                                if (template != null) {
                                    fields.add(template)
                                }
                            } else {
                                fields.add(CycleObjectGene(element.tableType))

                            }
                        }
                    }
                }
            }

        }
        return ObjectGene(tableName, fields, tableName)
    }

    private fun createInputObjectGene(tableName: String,
                                      tableType: String,
                                      kindOfTableType: String,
                                      history: Deque<String> = ArrayDeque<String>(),
                                      isKindOfTableTypeOptional: Boolean,
                                      isKindOfKindOfTableFieldOptional: Boolean
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        for (element in argsTables) {
            if (element.tableName == tableName) {
                if (element.kindOfTableType.toString().equals("SCALAR", ignoreCase = true)) {
                    val field = element.tableField
                    val template = field?.let {
                        getGene(tableName, element.tableType, kindOfTableType, it, history,
                                element.isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                    }
                    if (template != null) {
                        fields.add(template)
                    }
                } else {
                    if (element.kindOfTableField.toString().equals("LIST", ignoreCase = true)) {
                        val template = getGene(element.tableType, element.kindOfTableField.toString(),
                                element.kindOfTableType.toString(),
                                element.tableType, history, isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)

                        if (template != null) {
                            fields.add(template)
                        }
                    } else {
                        if (element.kindOfTableType.toString().equals("INPUT_OBJECT", ignoreCase = true)) {
                            history.add(element.tableName)
                            if (history.count { it == element.tableName } == 1) {
                                val template = getGene(element.tableType, element.kindOfTableType.toString(), element.kindOfTableField.toString(),
                                        element.tableType, history, isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                                if (template != null) {
                                    fields.add(template)
                                }
                            } else {
                                fields.add(CycleObjectGene(element.tableType))

                            }
                        }
                    }
                }
            }

        }
        return ObjectGene(tableName, fields, tableName)
    }

}