package org.evomaster.core.problem.graphql.builder

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.KindX
import org.evomaster.core.problem.graphql.schema.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object StateBuilder {

    private val log: Logger = LoggerFactory.getLogger(StateBuilder::class.java)

    /**
     * Specific terms/keywords used in the generated JSON schema retrieved
     * with an introspective query
     */
    private val systemTypes = listOf(
            "__Schema", "__Directive", "__DirectiveLocation", "__EnumValue",
            "__Field", "__InputValue", "__Type", "__TypeKind"
    )

    fun initTablesInfo(schemaObj: SchemaObj) : TempState{

        val state = TempState()

        for (elementIntypes in schemaObj.data.__schema.types) {
            if (systemTypes.contains(elementIntypes.name)) {
                continue
            }

            for (elementInfields in elementIntypes.fields.orEmpty()) {
                /**
                 * extracting tables
                 */
                val tableElement = Table()
                tableElement.fieldName = elementInfields.name

                if (elementInfields.type.kind == __TypeKind.NON_NULL) {// non optional list or object or scalar

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
                        inputElement.typeName = elementInfields.name
                        if (elementInArgs.type.kind == __TypeKind.NON_NULL) //non optional list or object or scalar or enum
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
        state.argsTables =
                state.argsTables.distinctBy { Pair(it.typeName, it.fieldName) }.toMutableList()//remove redundant elements
        state.tables =
                state.tables.distinctBy { Pair(it.typeName, it.fieldName) }.toMutableList()//remove redundant elements

        initTablesIndexedByName(state)

        return state
    }

    private fun initTablesIndexedByName(state: TempState){

        val names = state.tables.map { it.typeName }.filterNotNull().toSet()

        for(n in names) {
            val list = state.tables.filter { it.typeName == n }
            state.tablesIndexedByName[n] = list
        }
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

        if (k.kind0 == __TypeKind.LIST) {//optional list in the top
            tableElement.kindOfTableField = __TypeKind.LIST
            tableElement.isKindOfTableFieldOptional = true
            if (k.kind1 == __TypeKind.NON_NULL) {// non optional object or scalar or enum or union or interface
                tableElement.isKindOfTableFieldTypeOptional = false
                if (k.kind2?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.typeName = elementInTypes.name
                    state.tables.add(tableElement)
                }

            } else {//optional object or scalar or enum or union or interface

                tableElement.isKindOfTableFieldTypeOptional = true
                if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind1
                    tableElement.tableFieldType = elementInfields.type.ofType.name
                    tableElement.typeName = elementInTypes.name
                    state.tables.add(tableElement)
                }
            }

        } else {
            tableElement.isKindOfTableFieldTypeOptional = true
            if (k.kind0?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {// optional object or scalar or enum or union or interface in the top
                tableElement.kindOfTableFieldType = k.kind0
                tableElement.tableFieldType = elementInfields.type.name
                tableElement.typeName = elementInTypes.name
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

        if (k.kind1 == __TypeKind.LIST) {// non optional list
            tableElement.kindOfTableField = __TypeKind.LIST

            if (k.kind2 == __TypeKind.NON_NULL) {// non optional object or scalar or enum or union or interface
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = k.kind3
                tableElement.tableFieldType = elementInfields.type.ofType.ofType.ofType.name
                tableElement.typeName = elementIntypes.name
                state.tables.add(tableElement)
            } else {//optional object or scalar or enum or union or interface
                if (elementInfields?.type?.ofType?.ofType?.name == null) {
                    LoggingUtil.uniqueWarn(log, "Depth not supported yet $elementIntypes")
                } else {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.typeName = elementIntypes.name
                    state.tables.add(tableElement)
                }
            }
        } else if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
            tableElement.kindOfTableFieldType = k.kind1
            tableElement.tableFieldType = elementInfields.type.ofType.name
            tableElement.typeName = elementIntypes.name
            state.tables.add(tableElement)
        } else {
            LoggingUtil.uniqueWarn(log, "Type not supported yet:  ${elementInfields.type.ofType.kind}")
        }

    }

    private fun isKindObjOrScaOrEnumOrUniOrInter(kind: __TypeKind) =
            kind == __TypeKind.OBJECT || kind == __TypeKind.SCALAR || kind == __TypeKind.ENUM || kind == __TypeKind.UNION || kind == __TypeKind.INTERFACE

    /*
      This when an entry is not optional in argsTables
       */
    private fun handleNonOptionalInArgsTables(inputElement: Table, elementInArgs: InputValue, state: TempState) {

        val k = KindX(null, null, null, null)
        k.quadKindsInInputs(elementInArgs)

        if (k.kind1 == __TypeKind.LIST) {//non optional list
            inputElement.kindOfTableField = __TypeKind.LIST
            inputElement.isKindOfTableFieldOptional = false
            if (k.kind2 == __TypeKind.NON_NULL) {// non optional input object or scalar
                if (elementInArgs.type.ofType.ofType.ofType.kind == __TypeKind.INPUT_OBJECT) {// non optional input object
                    inputElement.kindOfTableFieldType = __TypeKind.INPUT_OBJECT
                    inputElement.isKindOfTableFieldTypeOptional = false
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.ofType.name
                    inputElement.fieldName = elementInArgs.name
                    state.argsTables.add(inputElement)
                } else {// non optional scalar or enum
                    if (k.kind3 == __TypeKind.SCALAR || k.kind3 == __TypeKind.ENUM) {
                        inputElement.kindOfTableFieldType = __TypeKind.SCALAR
                        inputElement.isKindOfTableFieldTypeOptional = false
                        inputElement.tableFieldType = elementInArgs.type.ofType.ofType.ofType.name
                        inputElement.fieldName = elementInArgs.name
                        state.argsTables.add(inputElement)
                    }
                }
            } else { // optional input object or scalar or enum
                inputElement.isKindOfTableFieldTypeOptional = true
                if (isKindInpuObjOrScaOrEnum(k.kind1!!)) {
                    inputElement.kindOfTableFieldType = k.kind2
                    inputElement.isKindOfTableFieldTypeOptional = true
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.name
                    inputElement.fieldName = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
            }
        } else // non optional input object or scalar or enum not in a list
            if (k.kind1?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                inputElement.kindOfTableFieldType = k.kind1
                inputElement.isKindOfTableFieldTypeOptional = false
                inputElement.tableFieldType = elementInArgs.type.ofType.name
                inputElement.fieldName = elementInArgs.name
                state.argsTables.add(inputElement)
            }
    }

    /*
       This when an entry is optional in argsTables
    */
    private fun handleOptionalInArgsTables(inputElement: Table, elementInArgs: InputValue, state: TempState) {

        val k = KindX(null, null, null, null)
        k.quadKindsInInputs(elementInArgs)

        if (k.kind0 == __TypeKind.LIST) {//optional list in the top
            inputElement.kindOfTableField = __TypeKind.LIST
            inputElement.isKindOfTableFieldOptional = true
            if (k.kind1 == __TypeKind.NON_NULL) {// non optional input object or scalar
                if (k.kind2?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                    inputElement.kindOfTableFieldType = k.kind2
                    inputElement.isKindOfTableFieldTypeOptional = false
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.name
                    inputElement.fieldName = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
            } else //optional input object or scalar or enum
                if (k.kind1?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                    inputElement.kindOfTableFieldType = k.kind1
                    inputElement.isKindOfTableFieldTypeOptional = true
                    inputElement.tableFieldType = elementInArgs.type.ofType.name
                    inputElement.fieldName = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
        } else // optional input object or scalar or enum in the top
            if (k.kind0?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                inputElement.kindOfTableFieldType = k.kind0
                inputElement.isKindOfTableFieldTypeOptional = true
                inputElement.tableFieldType = elementInArgs.type.name
                inputElement.fieldName = elementInArgs.name
                state.argsTables.add(inputElement)
            }
    }

    private fun isKindInpuObjOrScaOrEnum(kind: __TypeKind) = kind == __TypeKind.INPUT_OBJECT || kind == __TypeKind.SCALAR || kind == __TypeKind.ENUM

    /*
      Extract tempArgsTables
          */
    private fun extractTempArgsTables(state: TempState, schemaObj: SchemaObj) {
        for (elementInInputParamTable in state.argsTables) {
            if (elementInInputParamTable.kindOfTableFieldType == __TypeKind.INPUT_OBJECT) {
                for (elementIntypes in schemaObj.data.__schema.types) {
                    if ((elementInInputParamTable.tableFieldType == elementIntypes.name) && (elementIntypes.kind == __TypeKind.INPUT_OBJECT))
                        for (elementInInputFields in elementIntypes.inputFields) {
                            val kind0 = elementInInputFields.type.kind
                            val kind1 = elementInInputFields?.type?.ofType?.kind
                            if (kind0 == __TypeKind.NON_NULL) {//non optional scalar or enum
                                if (kind1 == __TypeKind.SCALAR || kind1 == __TypeKind.ENUM) {// non optional scalar or enum
                                    val inputElement = Table()
                                    inputElement.typeName = elementIntypes.name
                                    inputElement.kindOfTableFieldType = kind1
                                    inputElement.isKindOfTableFieldTypeOptional = false
                                    inputElement.tableFieldType = elementInInputFields.type.ofType.name
                                    inputElement.fieldName = elementInInputFields.name
                                    state.tempArgsTables.add(inputElement)
                                }
                            } else // optional scalar or enum
                                if (kind0 == __TypeKind.SCALAR || kind0 == __TypeKind.ENUM) {// optional scalar or enum
                                    val inputElement = Table()
                                    inputElement.typeName = elementIntypes.name
                                    inputElement.kindOfTableFieldType = kind0
                                    inputElement.isKindOfTableFieldTypeOptional = true
                                    inputElement.tableFieldType = elementInInputFields.type.name
                                    inputElement.fieldName = elementInInputFields.name
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
                if ((elementInInputParamTable.kindOfTableFieldType == __TypeKind.ENUM) && (elementIntypes.kind == __TypeKind.ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
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
                if ((elementInInputParamTable.kindOfTableFieldType == __TypeKind.ENUM) && (elementIntypes.kind == __TypeKind.ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
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
                if ((elementInTable.kindOfTableFieldType == __TypeKind.UNION) && (elementIntypes.kind == __TypeKind.UNION) && (elementIntypes.name == elementInTable.tableFieldType)) {
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
                if (elementInTable.kindOfTableFieldType == __TypeKind.UNION) {
                    for (elementInUnion in elementInTable.unionTypes) {//for each object in the union
                        if ((elementIntypes.kind == __TypeKind.OBJECT) && (elementIntypes.name == elementInUnion)) {
                            for (elementInfields in elementIntypes.fields.orEmpty()) {//Construct the table elements for this object
                                val tableElement = Table()
                                tableElement.fieldName = elementInfields.name//eg:Page

                                if (elementInfields.type.kind == __TypeKind.NON_NULL) {// non optional list or object or scalar

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
        state.tempUnionTables = state.tempUnionTables.distinctBy { Pair(it.typeName, it.fieldName) }
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

        if (k.kind0 == __TypeKind.LIST) {//optional list in the top
            tableElement.kindOfTableField = __TypeKind.LIST
            tableElement.isKindOfTableFieldOptional = true
            if (k.kind1 == __TypeKind.NON_NULL) {// non optional object or scalar or enum or union
                tableElement.isKindOfTableFieldTypeOptional = false
                if (k.kind2?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.typeName = elementInTypes.name
                    state.tempUnionTables.add(tableElement)
                }

            } else {
                tableElement.isKindOfTableFieldTypeOptional = true
                if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {//optional object or scalar or enum or union
                    tableElement.kindOfTableFieldType = k.kind1
                    tableElement.tableFieldType = elementInfields.type.ofType.name
                    tableElement.typeName = elementInTypes.name
                    state.tempUnionTables.add(tableElement)
                }
            }

        } else {
            tableElement.isKindOfTableFieldTypeOptional = true
            if (k.kind0?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {// optional object or scalar or enum in the top
                tableElement.kindOfTableFieldType = k.kind0
                tableElement.tableFieldType = elementInfields.type.name
                tableElement.typeName = elementInTypes.name
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

        if (k.kind1 == __TypeKind.LIST) {// non optional list
            tableElement.kindOfTableField = __TypeKind.LIST

            if (k.kind2 == __TypeKind.NON_NULL) {// non optional object or scalar or enum
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = k.kind3
                tableElement.tableFieldType = elementInfields.type.ofType.ofType.ofType.name
                tableElement.typeName = elementIntypes.name
                state.tempUnionTables.add(tableElement)
            } else {//optional object or scalar or enum
                if (elementInfields?.type?.ofType?.ofType?.name == null) {
                    LoggingUtil.uniqueWarn(log, "Depth not supported yet ${elementIntypes}")
                } else {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.typeName = elementIntypes.name
                    state.tempUnionTables.add(tableElement)
                }
            }
        } else if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
            tableElement.kindOfTableFieldType = k.kind1
            tableElement.tableFieldType = elementInfields.type.ofType.name
            tableElement.typeName = elementIntypes.name
            state.tempUnionTables.add(tableElement)
        } else {
            LoggingUtil.uniqueWarn(log, "Type not supported yet:  ${elementInfields.type.ofType.kind}")
        }

    }

    private fun handleEnumInTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.tables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == __TypeKind.ENUM) && (elementIntypes.kind == __TypeKind.ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
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
                if ((elementInTable.kindOfTableFieldType == __TypeKind.INTERFACE) && (elementIntypes.kind == __TypeKind.INTERFACE) && (elementIntypes.name == elementInTable.tableFieldType)) {
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
}