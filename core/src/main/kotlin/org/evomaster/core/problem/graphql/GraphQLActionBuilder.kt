package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.graphql.schema.*
import org.evomaster.core.problem.graphql.schema.__TypeKind.*
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GraphQLActionBuilder {

    private val log: Logger = LoggerFactory.getLogger(GraphQLActionBuilder::class.java)
    private val idGenerator = AtomicInteger()

    private val systemTypes = listOf(
        "__Schema", "__Directive", "__DirectiveLocation", "__EnumValue",
        "__Field", "__InputValue", "__Type", "__TypeKind"
    )

    data class TempState(
        /**
         * A data structure used to store information extracted from the schema eg, Objects types.
         */
        var tables: MutableList<Table> = mutableListOf(),
        /**
         * A data structure used to store information extracted from the schema about input types eg, Input types.
         */
        val argsTables: MutableList<Table> = mutableListOf(),
        /*
        * An intermediate data structure used for extracting argsTables
       */
        val tempArgsTables: MutableList<Table> = mutableListOf(),
        /*
         * An intermediate data structure used for extracting Union types
         */
        var tempUnionTables: MutableList<Table> = mutableListOf()
    )

    /**
     * @param schema: the schema extracted from a GraphQL API, as a JSON string
     * @param actionCluster: for each mutation/query in the schema, populate this map with
     *                      new action templates.
     */
    fun addActionsFromSchema(schema: String, actionCluster: MutableMap<String, Action>) {

        val state = TempState()
        val gson = Gson()
        try {
            gson.fromJson(schema, SchemaObj::class.java)
        } catch (e: Exception) {
            throw SutProblemException("Failed to start the SUT, please check the GraphQl endpoint")
        }

        val schemaObj: SchemaObj = gson.fromJson(schema, SchemaObj::class.java)


        initTablesInfo(schemaObj, state)

        if (schemaObj.data.__schema.queryType != null && schemaObj.data.__schema.mutationType != null)
            for (element in state.tables) {
                /*
                In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
                 */
                if (element.tableType?.toLowerCase() == GqlConst.MUTATION || element.tableType?.toLowerCase() == GqlConst.QUERY || element.tableType?.toLowerCase() == GqlConst.ROOT || element?.tableType?.toLowerCase() == GqlConst.QUERY_TYPE) {
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
                        element.tableFieldWithArgs,
                        element.enumValues,
                        element.unionTypes,
                        element.interfaceTypes
                    )
                }
            }
        else if (schemaObj.data.__schema.queryType != null && schemaObj.data.__schema.mutationType == null)
            for (element in state.tables) {
                if (element.tableType?.toLowerCase() == GqlConst.QUERY || element.tableType?.toLowerCase() == GqlConst.ROOT || element.tableType?.toLowerCase() == GqlConst.QUERY_TYPE) {
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
                        element.tableFieldWithArgs,
                        element.enumValues,
                        element.unionTypes,
                        element.interfaceTypes
                    )
                }
            }
        else if (schemaObj.data.__schema.queryType == null && schemaObj.data.__schema.mutationType != null)
            for (element in state.tables) {
                if (element.tableType?.toLowerCase() == GqlConst.MUTATION) {
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
                        element.tableFieldWithArgs,
                        element.enumValues,
                        element.unionTypes,
                        element.interfaceTypes
                    )
                }
            }
        else if (schemaObj.data.__schema.queryType == null && schemaObj.data.__schema.mutationType == null)
            LoggingUtil.uniqueWarn(log, "No entrance for the schema")
    }

    fun initTablesInfo(schemaObj: SchemaObj, state: TempState) {

        for (elementIntypes in schemaObj.data.__schema.types) {
            if (systemTypes.contains(elementIntypes.name)) {
                continue
            }

            for (elementInfields in elementIntypes.fields.orEmpty()) {
                /**
                 * extracting tables
                 */
                val tableElement = Table()
                tableElement.tableField = elementInfields.name

                if (elementInfields.type.kind == NON_NULL) {// non optional list or object or scalar

                    handleNonOptionalInTables(elementInfields, tableElement, elementIntypes, state)

                } else {
                    handleOptionalInTables(elementInfields, tableElement, elementIntypes, state)
                }

                /*
                 * extracting argsTables: 1/2
                 */
                if (elementInfields.args.isNotEmpty()) {
                    tableElement.tableFieldWithArgs = true
                    for (elementInArgs in elementInfields.args) {
                        val inputElement = Table()
                        inputElement.tableType = elementInfields.name
                        if (elementInArgs.type.kind == NON_NULL) //non optional list or object or scalar or enum
                            handleNonOptionalInArgsTables(inputElement, elementInArgs, state)
                        else  //optional list or input object or scalar or enum
                            handleOptionalInArgsTables(inputElement, elementInArgs, state)

                    }
                }
            }
        }
        handleEnumInArgsTables(state, schemaObj)
        handleEnumInTables(state, schemaObj)
        /*
        extract and add union objects to tables
         */
        handleUnionInTables(state, schemaObj)
        /*
        extract and add interface objects to tables
         */
        handleInterfacesInTables(state, schemaObj)
        /*
         *extracting tempArgsTables, an intermediate table for extracting argsTables
         */
        extractTempArgsTables(state, schemaObj)
        handleEnumInTempArgsTables(state, schemaObj)
        /*
         * merging argsTables with tempArgsTables: extracting argsTables: 2/2
         */
        state.argsTables.addAll(state.tempArgsTables)
        state.tables =
            state.tables.distinctBy { Pair(it.tableType, it.tableField) }.toMutableList()//remove redundant elements
    }

    /*
    This when an entry is optional in Tables
    */
    private fun handleOptionalInTables(
        elementInfields: __Field,
        tableElement: Table,
        elementInTypes: FullType,
        state: TempState
    ) {

        /*
        *Note: the introspective query of GQl goes until 7 ofType. Here we go until 3 ofTypes since only 2 APIs go deeper.
         */
        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        if (k.kind0 == LIST) {//optional list in the top
            tableElement.kindOfTableField = LIST
            tableElement.isKindOfTableFieldOptional = true
            if (k.kind1 == NON_NULL) {// non optional object or scalar or enum or union or interface
                tableElement.isKindOfTableFieldTypeOptional = false
                if (k.kind2?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tables.add(tableElement)
                }

            } else {//optional object or scalar or enum or union or interface

                tableElement.isKindOfTableFieldTypeOptional = true
                if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind1
                    tableElement.tableFieldType = elementInfields.type.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tables.add(tableElement)
                }
            }

        } else {
            tableElement.isKindOfTableFieldTypeOptional = true
            if (k.kind0?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {// optional object or scalar or enum or union or interface in the top
                tableElement.kindOfTableFieldType = k.kind0
                tableElement.tableFieldType = elementInfields.type.name
                tableElement.tableType = elementInTypes.name
                state.tables.add(tableElement)
            }
        }

    }

    /*
        This is to handle entries that are NOT optional, and must be there, ie, they cannot be null
     */
    private fun handleNonOptionalInTables(
        elementInfields: __Field,
        tableElement: Table,
        elementIntypes: FullType,
        state: TempState
    ) {

        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        tableElement.isKindOfTableFieldOptional = false

        if (k.kind1 == LIST) {// non optional list
            tableElement.kindOfTableField = LIST

            if (k.kind2 == NON_NULL) {// non optional object or scalar or enum or union or interface
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = k.kind3
                tableElement.tableFieldType = elementInfields.type.ofType.ofType.ofType.name
                tableElement.tableType = elementIntypes.name
                state.tables.add(tableElement)
            } else {//optional object or scalar or enum or union or interface
                if (elementInfields?.type?.ofType?.ofType?.name == null) {
                    LoggingUtil.uniqueWarn(log, "Depth not supported yet ${elementIntypes}")
                } else {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementIntypes.name
                    state.tables.add(tableElement)
                }
            }
        } else if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
            tableElement.kindOfTableFieldType = k.kind1
            tableElement.tableFieldType = elementInfields.type.ofType.name
            tableElement.tableType = elementIntypes.name
            state.tables.add(tableElement)
        } else {
            LoggingUtil.uniqueWarn(log, "Type not supported yet:  ${elementInfields.type.ofType.kind}")
        }

    }

    private fun isKindObjOrScaOrEnumOrUniOrInter(kind: __TypeKind) =
        kind == OBJECT || kind == SCALAR || kind == ENUM || kind == UNION || kind == INTERFACE

    /*
      This when an entry is not optional in argsTables
       */
    private fun handleNonOptionalInArgsTables(inputElement: Table, elementInArgs: InputValue, state: TempState) {

        val k = KindX(null, null, null, null)
        k.quadKindsInInputs(elementInArgs)

        if (k.kind1 == LIST) {//non optional list
            inputElement.kindOfTableField = LIST
            inputElement.isKindOfTableFieldOptional = false
            if (k.kind2 == NON_NULL) {// non optional input object or scalar
                if (elementInArgs.type.ofType.ofType.ofType.kind == INPUT_OBJECT) {// non optional input object
                    inputElement.kindOfTableFieldType = INPUT_OBJECT
                    inputElement.isKindOfTableFieldTypeOptional = false
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                } else {// non optional scalar or enum
                    if (k.kind3 == SCALAR || k.kind3 == ENUM) {
                        inputElement.kindOfTableFieldType = SCALAR
                        inputElement.isKindOfTableFieldTypeOptional = false
                        inputElement.tableFieldType = elementInArgs.type.ofType.ofType.ofType.name
                        inputElement.tableField = elementInArgs.name
                        state.argsTables.add(inputElement)
                    }
                }
            } else { // optional input object or scalar or enum
                inputElement.isKindOfTableFieldTypeOptional = true
                if (isKindInpuObjOrScaOrEnum(k.kind1!!)) {
                    inputElement.kindOfTableFieldType = k.kind2
                    inputElement.isKindOfTableFieldTypeOptional = true
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
            }
        } else // non optional input object or scalar or enum not in a list
            if (k.kind1?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                inputElement.kindOfTableFieldType = k.kind1
                inputElement.isKindOfTableFieldTypeOptional = false
                inputElement.tableFieldType = elementInArgs.type.ofType.name
                inputElement.tableField = elementInArgs.name
                state.argsTables.add(inputElement)
            }
    }

    /*
       This when an entry is optional in argsTables
    */
    private fun handleOptionalInArgsTables(inputElement: Table, elementInArgs: InputValue, state: TempState) {

        val k = KindX(null, null, null, null)
        k.quadKindsInInputs(elementInArgs)

        if (k.kind0 == LIST) {//optional list in the top
            inputElement.kindOfTableField = LIST
            inputElement.isKindOfTableFieldOptional = true
            if (k.kind1 == NON_NULL) {// non optional input object or scalar
                if (k.kind2?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                    inputElement.kindOfTableFieldType = k.kind2
                    inputElement.isKindOfTableFieldTypeOptional = false
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
            } else //optional input object or scalar or enum
                if (k.kind1?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                    inputElement.kindOfTableFieldType = k.kind1
                    inputElement.isKindOfTableFieldTypeOptional = true
                    inputElement.tableFieldType = elementInArgs.type.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
        } else // optional input object or scalar or enum in the top
            if (k.kind0?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                inputElement.kindOfTableFieldType = k.kind0
                inputElement.isKindOfTableFieldTypeOptional = true
                inputElement.tableFieldType = elementInArgs.type.name
                inputElement.tableField = elementInArgs.name
                state.argsTables.add(inputElement)
            }
    }

    private fun isKindInpuObjOrScaOrEnum(kind: __TypeKind) = kind == INPUT_OBJECT || kind == SCALAR || kind == ENUM

    /*
      Extract tempArgsTables
          */
    private fun extractTempArgsTables(state: TempState, schemaObj: SchemaObj) {
        for (elementInInputParamTable in state.argsTables) {
            if (elementInInputParamTable.kindOfTableFieldType == INPUT_OBJECT) {
                for (elementIntypes in schemaObj.data.__schema.types) {
                    if ((elementInInputParamTable.tableFieldType == elementIntypes.name) && (elementIntypes.kind == INPUT_OBJECT))
                        for (elementInInputFields in elementIntypes.inputFields) {
                            val kind0 = elementInInputFields.type.kind
                            val kind1 = elementInInputFields?.type?.ofType?.kind
                            if (kind0 == NON_NULL) {//non optional scalar or enum
                                if (kind1 == SCALAR || kind1 == ENUM) {// non optional scalar or enum
                                    val inputElement = Table()
                                    inputElement.tableType = elementIntypes.name
                                    inputElement.kindOfTableFieldType = kind1
                                    inputElement.isKindOfTableFieldTypeOptional = false
                                    inputElement.tableFieldType = elementInInputFields.type.ofType.name
                                    inputElement.tableField = elementInInputFields.name
                                    state.tempArgsTables.add(inputElement)
                                }
                            } else // optional scalar or enum
                                if (kind0 == SCALAR || kind0 == ENUM) {// optional scalar or enum
                                    val inputElement = Table()
                                    inputElement.tableType = elementIntypes.name
                                    inputElement.kindOfTableFieldType = kind0
                                    inputElement.isKindOfTableFieldTypeOptional = true
                                    inputElement.tableFieldType = elementInInputFields.type.name
                                    inputElement.tableField = elementInInputFields.name
                                    state.tempArgsTables.add(inputElement)
                                }
                        }
                }

            }
        }
    }

    private fun handleEnumInArgsTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.argsTables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == ENUM) && (elementIntypes.kind == ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
                    val enumElement: MutableList<String> = mutableListOf()
                    for (elementInEnumValues in elementIntypes.enumValues) {
                        enumElement.add(elementInEnumValues.name)
                    }
                    allEnumElement.put(elementInInputParamTable.tableFieldType, enumElement)
                }
            }
        }
        for (elementInInputParamTable in state.argsTables) {

            for (elemntInAllEnumElement in allEnumElement) {

                if (elementInInputParamTable.tableFieldType == elemntInAllEnumElement.key)

                    for (elementInElementInAllEnumElement in elemntInAllEnumElement.value) {

                        elementInInputParamTable.enumValues.add(elementInElementInAllEnumElement)
                    }
            }
        }
    }

    private fun handleEnumInTempArgsTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.tempArgsTables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == ENUM) && (elementIntypes.kind == ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
                    val enumElement: MutableList<String> = mutableListOf()
                    for (elementInEnumValues in elementIntypes.enumValues) {
                        enumElement.add(elementInEnumValues.name)
                    }
                    allEnumElement.put(elementInInputParamTable.tableFieldType, enumElement)
                }
            }
        }
        for (elementInInputParamTable in state.tempArgsTables) {

            for (elemntInAllEnumElement in allEnumElement) {

                if (elementInInputParamTable.tableFieldType == elemntInAllEnumElement.key)

                    for (elementInElementInAllEnumElement in elemntInAllEnumElement.value) {

                        elementInInputParamTable.enumValues.add(elementInElementInAllEnumElement)
                    }
            }
        }
    }

    private fun handleUnionInTables(state: TempState, schemaObj: SchemaObj) {
        val allUnionElement: MutableMap<String, MutableList<String>> = mutableMapOf()

        for (elementInTable in state.tables) {//extraction of the union object names in a map
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInTable.kindOfTableFieldType == UNION) && (elementIntypes.kind == UNION) && (elementIntypes.name == elementInTable.tableFieldType)) {
                    val unionElement: MutableList<String> = mutableListOf()
                    for (elementInUnionTypes in elementIntypes.possibleTypes) {
                        unionElement.add(elementInUnionTypes.name)//get the name of the obj_n
                    }
                    allUnionElement.put(elementInTable.tableFieldType, unionElement)
                }
            }
        }
        for (elementInTable in state.tables) {//Insertion of the union objects names map in the tables

            for (elementInAllUnionElement in allUnionElement) {

                if (elementInTable.tableFieldType == elementInAllUnionElement.key)

                    for (elementInElementInAllUnionElement in elementInAllUnionElement.value) {

                        elementInTable.unionTypes.add(elementInElementInAllUnionElement)
                    }
            }
        }

        /*adding every union object in the tables
        todo check if needed
        * */
        for (elementIntypes in schemaObj.data.__schema.types) {
            if (systemTypes.contains(elementIntypes.name)) {
                continue
            }
            for (elementInTable in state.tables) {//for each union in the table
                if (elementInTable.kindOfTableFieldType == UNION) {
                    for (elementInUnion in elementInTable.unionTypes) {//for each object in the union
                        if ((elementIntypes.kind == OBJECT) && (elementIntypes.name == elementInUnion)) {
                            for (elementInfields in elementIntypes.fields.orEmpty()) {//Construct the table elements for this object
                                val tableElement = Table()
                                tableElement.tableField = elementInfields.name//eg:Page

                                if (elementInfields.type.kind == NON_NULL) {// non optional list or object or scalar

                                    handleNonOptionalInTempUnionTables(
                                        elementInfields,
                                        tableElement,
                                        elementIntypes,
                                        state
                                    )//uses the: tempUnionTables

                                } else {
                                    handleOptionalInTempUnionTables(
                                        elementInfields,
                                        tableElement,
                                        elementIntypes,
                                        state
                                    )//uses the: tempUnionTables
                                }
                            }
                        }
                    }
                }
            }
        }
        state.tempUnionTables = state.tempUnionTables.distinctBy { Pair(it.tableType, it.tableField) }
            .toMutableList()//remove redundant elements from tempUnionTables
        /*
        * merging tempUnionTables with tables
        */
        state.tables.addAll(state.tempUnionTables)
    }

    private fun handleOptionalInTempUnionTables(
        elementInfields: __Field,
        tableElement: Table,
        elementInTypes: FullType,
        state: TempState
    ) {
        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        if (k.kind0 == LIST) {//optional list in the top
            tableElement.kindOfTableField = LIST
            tableElement.isKindOfTableFieldOptional = true
            if (k.kind1 == NON_NULL) {// non optional object or scalar or enum or union
                tableElement.isKindOfTableFieldTypeOptional = false
                if (k.kind2?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tempUnionTables.add(tableElement)
                }

            } else {
                tableElement.isKindOfTableFieldTypeOptional = true
                if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {//optional object or scalar or enum or union
                    tableElement.kindOfTableFieldType = k.kind1
                    tableElement.tableFieldType = elementInfields.type.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tempUnionTables.add(tableElement)
                }
            }

        } else {
            tableElement.isKindOfTableFieldTypeOptional = true
            if (k.kind0?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {// optional object or scalar or enum in the top
                tableElement.kindOfTableFieldType = k.kind0
                tableElement.tableFieldType = elementInfields.type.name
                tableElement.tableType = elementInTypes.name
                state.tempUnionTables.add(tableElement)
            }
        }

    }

    private fun handleNonOptionalInTempUnionTables(
        elementInfields: __Field,
        tableElement: Table,
        elementIntypes: FullType,
        state: TempState
    ) {

        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        tableElement.isKindOfTableFieldOptional = false

        if (k.kind1 == LIST) {// non optional list
            tableElement.kindOfTableField = LIST

            if (k.kind2 == NON_NULL) {// non optional object or scalar or enum
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = k.kind3
                tableElement.tableFieldType = elementInfields.type.ofType.ofType.ofType.name
                tableElement.tableType = elementIntypes.name
                state.tempUnionTables.add(tableElement)
            } else {//optional object or scalar or enum
                if (elementInfields?.type?.ofType?.ofType?.name == null) {
                    LoggingUtil.uniqueWarn(log, "Depth not supported yet ${elementIntypes}")
                } else {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementIntypes.name
                    state.tempUnionTables.add(tableElement)
                }
            }
        } else if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
            tableElement.kindOfTableFieldType = k.kind1
            tableElement.tableFieldType = elementInfields.type.ofType.name
            tableElement.tableType = elementIntypes.name
            state.tempUnionTables.add(tableElement)
        } else {
            LoggingUtil.uniqueWarn(log, "Type not supported yet:  ${elementInfields.type.ofType.kind}")
        }

    }

    private fun handleEnumInTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.tables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == ENUM) && (elementIntypes.kind == ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
                    val enumElement: MutableList<String> = mutableListOf()
                    for (elementInEnumValues in elementIntypes.enumValues) {
                        enumElement.add(elementInEnumValues.name)
                    }
                    allEnumElement.put(elementInInputParamTable.tableFieldType, enumElement)
                }
            }
        }
        for (elementInInputParamTable in state.tables) {

            for (elemntInAllEnumElement in allEnumElement) {

                if (elementInInputParamTable.tableFieldType == elemntInAllEnumElement.key)

                    for (elementInElementInAllEnumElement in elemntInAllEnumElement.value) {

                        elementInInputParamTable.enumValues.add(elementInElementInAllEnumElement)
                    }
            }
        }
    }

    private fun handleInterfacesInTables(state: TempState, schemaObj: SchemaObj) {
        val allInterfaceElement: MutableMap<String, MutableList<String>> = mutableMapOf()

        for (elementInTable in state.tables) {//extraction of the interface object names in a map
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInTable.kindOfTableFieldType == INTERFACE) && (elementIntypes.kind == INTERFACE) && (elementIntypes.name == elementInTable.tableFieldType)) {
                    val interfaceElement: MutableList<String> = mutableListOf()
                    for (elementInInterfaceTypes in elementIntypes.possibleTypes) {
                        interfaceElement.add(elementInInterfaceTypes.name)//get the name of the obj_n
                    }
                    allInterfaceElement.put(elementInTable.tableFieldType, interfaceElement)
                }
            }
        }
        for (elementInTable in state.tables) {//Insertion of the union objects names map in the tables

            for (elementInAllInterfaceElement in allInterfaceElement) {

                if (elementInTable.tableFieldType == elementInAllInterfaceElement.key)

                    for (elementInElementInAllInterfaceElement in elementInAllInterfaceElement.value) {

                        elementInTable.interfaceTypes.add(elementInElementInAllInterfaceElement)
                    }
            }
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
        interfaceTypes: MutableList<String>
    ) {
        if (methodName == null) {
            log.warn("Skipping operation, as no method name is defined.")
            return;
        }
        if (methodType == null) {
            log.warn("Skipping operation, as no method type is defined.")
            return;
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
                //TODO log warn
                return
            }
        }

        val actionId = "$methodName${idGenerator.incrementAndGet()}"

        val params = extractParams(
            state, methodName, tableFieldType, kindOfTableFieldType, kindOfTableField,
            tableType, isKindOfTableFieldTypeOptional,
            isKindOfTableFieldOptional, tableFieldWithArgs, enumValues, unionTypes, interfaceTypes
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

        //Create the action
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
        tableFieldWithArgs: Boolean,
        enumValues: MutableList<String>,
        unionTypes: MutableList<String>,
        interfaceTypes: MutableList<String>

    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque<String>()

        if (tableFieldWithArgs) {

            for (element in state.argsTables) {

                if (element.tableType == methodName) {

                    if (element.kindOfTableFieldType == SCALAR || element.kindOfTableFieldType == ENUM) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                        val gene = getInputScalarListOrEnumListGene(
                            state,
                            element.tableFieldType,
                            element.kindOfTableField.toString(),
                            element.kindOfTableFieldType.toString(),
                            element.tableType.toString(),
                            history,
                            element.isKindOfTableFieldTypeOptional,
                            element.isKindOfTableFieldOptional,
                            element.enumValues,
                            element.tableField
                        )
                        params.add(GQInputParam(element.tableField, gene))

                    } else {//for input objects types and objects types
                        val gene = getInputGene(
                            state,
                            element.tableFieldType,
                            element.kindOfTableField.toString(),
                            element.kindOfTableFieldType.toString(),
                            element.tableType.toString(),
                            history,
                            element.isKindOfTableFieldTypeOptional,
                            element.isKindOfTableFieldOptional,
                            element.enumValues,
                            element.tableField,
                            element.unionTypes,
                            element.interfaceTypes
                        )
                        params.add(GQInputParam(element.tableField, gene))
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
                interfaceTypes
            )

            //Remove primitive types (scalar and enum) from return params
            if (gene.name.toLowerCase() != "scalar"
                && !(gene is OptionalGene && gene.gene.name == "scalar")
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.name.toLowerCase() == "scalar")
                && !(gene is ArrayGene<*> && gene.template.name.toLowerCase() == "scalar")
                && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.name.toLowerCase() == "scalar")
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template.name.toLowerCase() == "scalar")
                //enum cases
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.gene is EnumGene<*>)
                && !(gene is ArrayGene<*> && gene.template is EnumGene<*>)
                && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.gene is EnumGene<*>)
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is EnumGene<*>)
                && !(gene is EnumGene<*>)
                && !(gene is OptionalGene && gene.gene is EnumGene<*>)

            ) {
                params.add(GQReturnParam(methodName, gene))
            }

        } else {
            //The action does not contain arguments, it only contain a return type
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
                interfaceTypes
            )

            //Remove primitive types (scalar and enum) from return params
            if (gene.name.toLowerCase() != "scalar"
                && !(gene is OptionalGene && gene.gene.name == "scalar")
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.name.toLowerCase() == "scalar")
                && !(gene is ArrayGene<*> && gene.template.name.toLowerCase() == "scalar")
                && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.name.toLowerCase() == "scalar")
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template.name.toLowerCase() == "scalar")
                //enum cases
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.gene is EnumGene<*>)
                && !(gene is ArrayGene<*> && gene.template is EnumGene<*>)
                && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.gene is EnumGene<*>)
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is EnumGene<*>)
                && !(gene is EnumGene<*>)
                && !(gene is OptionalGene && gene.gene is EnumGene<*>)

            ) {
                params.add(GQReturnParam(methodName, gene))
            }

        }

        return params
    }

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
        methodName: String
    ): Gene {

        when (kindOfTableField?.toLowerCase()) {
            GqlConst.LIST ->
                return if (isKindOfTableFieldOptional) {
                    val template = getInputScalarListOrEnumListGene(
                        state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName
                    )
                    OptionalGene(methodName, ArrayGene(tableType, template))
                } else {
                    val template = getInputScalarListOrEnumListGene(
                        state, tableType, kindOfTableFieldType, kindOfTableField, tableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName
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
                    isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName
                )
            "date" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, BooleanGene(methodName))
                else
                    DateGene(methodName)
            GqlConst.SCALAR ->
                return getInputScalarListOrEnumListGene(
                    state, tableFieldType, tableType, kindOfTableFieldType, kindOfTableField, history,
                    isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, enumValues, methodName
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
        interfaceTypes: MutableList<String>
    ): Gene {

        when (kindOfTableField?.toLowerCase()) {
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
                        interfaceTypes
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
                        interfaceTypes
                    )

                    ArrayGene(methodName, template)
                }
            GqlConst.OBJECT ->
                return if (isKindOfTableFieldTypeOptional) {
                    val optObjGene = createObjectGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName
                    )
                    OptionalGene(methodName, optObjGene)
                } else
                    createObjectGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName
                    )
            GqlConst.INPUT_OBJECT ->
                return if (isKindOfTableFieldTypeOptional) {
                    val optInputObjGene = createInputObjectGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName
                    )
                    OptionalGene(methodName, optInputObjGene)
                } else
                    createInputObjectGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName
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
                    interfaceTypes
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
                    interfaceTypes
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
        methodName: String
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        for (element in state.argsTables) {
            if (element.tableType == tableType) {

                if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.SCALAR) {
                    val field = element.tableField
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
                        element.interfaceTypes
                    )
                    fields.add(template)
                } else {
                    if (element.kindOfTableField.toString().toLowerCase() == GqlConst.LIST) {
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
                            element.tableField,
                            element.unionTypes,
                            element.interfaceTypes
                        )

                        fields.add(template)
                    } else
                        if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.INPUT_OBJECT) {
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
                                element.tableField,
                                element.unionTypes,
                                element.interfaceTypes
                            )

                            fields.add(template)

                        } else if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.ENUM) {
                            val field = element.tableField
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
                                element.interfaceTypes
                            )
                            fields.add(template)
                        }
                }

            }
        }
        return ObjectGene(methodName, fields, tableType)
    }

    /**
     *Extract the return gene: representing the return value in the GQL query/mutation.
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
        interfaceTypes: MutableList<String>
    ): Gene {

        when (kindOfTableField?.toLowerCase()) {
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
                    interfaceTypes
                )

                return OptionalGene(methodName, ArrayGene(tableType, template))
            }
            GqlConst.OBJECT -> {
                history.addLast(tableType)
                return if (history.count { it == tableType } == 1) {
                    val objGene = createObjectGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName
                    )
                    history.removeLast()
                    OptionalGene(methodName, objGene)
                } else {
                    history.removeLast()
                    (OptionalGene(methodName, CycleObjectGene(methodName)))
                }
            }
            GqlConst.UNION -> {
                history.addLast(tableType)
                return if (history.count { it == tableType } == 1) {
                    val optObjGene = createUnionObjectsGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, unionTypes
                    )
                    history.removeLast()
                    OptionalGene("$methodName#UNION#", optObjGene)
                } else {
                    history.removeLast()
                    (OptionalGene(methodName, CycleObjectGene(methodName)))
                }
            }
            GqlConst.INTERFACE -> {
                history.addLast(tableType)

                return if (history.count { it == tableType } == 1) {
                    //will contain basic interface fields, and had as name the methode name
                    var interfaceBaseOptObjGene = createObjectGene(
                        state, tableType, kindOfTableFieldType, history,
                        isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName
                    )

                    interfaceBaseOptObjGene = interfaceBaseOptObjGene as ObjectGene

                    interfaceBaseOptObjGene.name = interfaceBaseOptObjGene.name.plus("#BASE#")

                    //will contain additional interface fields, and had as name the name of the objects
                    val interfaceAdditionalOptObjGene = createInterfaceObjectGene(
                        state,
                        kindOfTableFieldType,
                        history,
                        isKindOfTableFieldTypeOptional,
                        isKindOfTableFieldOptional,
                        interfaceTypes,
                        interfaceBaseOptObjGene
                    )

                    //merge basic interface fields with additional interface fields
                    interfaceAdditionalOptObjGene.add(OptionalGene("$methodName#BASE#", interfaceBaseOptObjGene))

                    //will return a single optional object gene with optional basic interface fields and optional additional interface fields
                    OptionalGene(
                        "$methodName#INTERFACE#",
                        ObjectGene("$methodName#INTERFACE#", interfaceAdditionalOptObjGene)
                    )
                } else {
                    history.removeLast()
                    (OptionalGene(methodName, CycleObjectGene(methodName)))
                }
            }
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
                    interfaceTypes
                )
            "date" ->
                return OptionalGene(tableType, DateGene(tableType))
            GqlConst.ENUM ->
                return OptionalGene(tableType, EnumGene(tableType, enumValues))
            GqlConst.SCALAR ->
                return getReturnGene(
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
                    interfaceTypes
                )
            "id" ->
                return OptionalGene(tableType, StringGene(tableType))

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
        methodName: String
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()

        for (element in state.tables) {
            if (element.tableType == tableType) {
                if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.SCALAR) {
                    val field = element.tableField
                    val template = getReturnGene(
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
                        element.interfaceTypes
                    )
                    fields.add(template)
                } else {
                    if (element.kindOfTableField.toString().toLowerCase() == GqlConst.LIST) {
                        val template =
                            getReturnGene(
                                state,
                                element.tableFieldType,
                                element.kindOfTableField.toString(),
                                element.kindOfTableFieldType.toString(),
                                element.tableFieldType,
                                history,
                                isKindOfTableFieldTypeOptional,
                                isKindOfTableFieldOptional,
                                element.enumValues,
                                element.tableField,
                                element.unionTypes,
                                element.interfaceTypes
                            )

                        fields.add(template)
                    } else
                        if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.OBJECT) {
                            val template =
                                getReturnGene(
                                    state,
                                    element.tableFieldType,
                                    element.kindOfTableFieldType.toString(),
                                    element.kindOfTableField.toString(),
                                    element.tableFieldType,
                                    history,
                                    isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    element.enumValues,
                                    element.tableField,
                                    element.unionTypes,
                                    element.interfaceTypes
                                )

                            fields.add(template)

                        } else if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.ENUM) {
                            val field = element.tableField
                            val template = getReturnGene(
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
                                element.interfaceTypes
                            )

                            fields.add(template)

                        } else if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.UNION) {
                            val template =
                                getReturnGene(
                                    state,
                                    element.tableFieldType,
                                    element.kindOfTableFieldType.toString(),
                                    element.kindOfTableField.toString(),
                                    element.tableFieldType,
                                    history,
                                    isKindOfTableFieldTypeOptional,
                                    isKindOfTableFieldOptional,
                                    element.enumValues,
                                    element.tableField,
                                    element.unionTypes,
                                    element.interfaceTypes
                                )

                            fields.add(template)

                        } else
                            if (element.kindOfTableFieldType.toString().toLowerCase() == GqlConst.INTERFACE) {
                                val template =
                                    getReturnGene(
                                        state,
                                        element.tableFieldType,
                                        element.kindOfTableFieldType.toString(),
                                        element.kindOfTableField.toString(),
                                        element.tableFieldType,
                                        history,
                                        isKindOfTableFieldTypeOptional,
                                        isKindOfTableFieldOptional,
                                        element.enumValues,
                                        element.tableField,
                                        element.unionTypes,
                                        element.interfaceTypes
                                    )

                                fields.add(template)

                            }
                }
            }

        }
        return ObjectGene(methodName, fields, tableType)
    }

    private fun createUnionObjectsGene(
        state: TempState,
        tableType: String,
        kindOfTableFieldType: String,
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        methodName: String,
        unionTypes: MutableList<String>
    ): Gene {

        val fields: MutableList<Gene> = mutableListOf()

        for (elementInUnionTypes in unionTypes) {//Browse all objects defining the union
            history.addLast(elementInUnionTypes)

            val objGeneTemplate = createObjectGene(
                state, elementInUnionTypes, kindOfTableFieldType, history,
                isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, elementInUnionTypes
            )

            history.removeLast()
            fields.add(OptionalGene(objGeneTemplate.name, objGeneTemplate))
        }
        return ObjectGene("$methodName#UNION#", fields, tableType)
    }

    private fun createInterfaceObjectGene(
        state: TempState,
        kindOfTableFieldType: String,
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        interfaceTypes: MutableList<String>,
        interfaceBaseOptObjGene: Gene
    ): MutableList<Gene> {

        val fields: MutableList<Gene> = mutableListOf()

        for (elementInInterfaceTypes in interfaceTypes) {//Browse all additional objects in the interface

            history.addLast(elementInInterfaceTypes)
            val objGeneTemplate = createObjectGene(
                state, elementInInterfaceTypes, kindOfTableFieldType, history,
                isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, elementInInterfaceTypes
            )
            history.removeLast()

            var myObjGne = objGeneTemplate as ObjectGene//To object

            myObjGne = myObjGne.copyFields(interfaceBaseOptObjGene as ObjectGene)//remove useless fields

            if (myObjGne.fields.isNotEmpty())
                fields.add(OptionalGene(myObjGne.name, myObjGne))
        }
        return fields
    }

}

