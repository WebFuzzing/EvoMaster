package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
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


        val tables = initTableInfo(schemaObj)


        for (element in tables) {

            if (element.tableName == "Mutation" || element.tableName == "Query") {

                handleOperation(actionCluster,
                        element.tableField,
                        element.tableName,
                        element.tableType,
                        element.kindOfTableType.toString(),
                        element.kindOfTableField.toString(),
                        tables,
                        element.tableName.toString(),
                        element.isKindOfTableTypeOptional,
                        element.isKindOfKindOfTableFieldOptional)

            }
        }

    }

    private fun initTableInfo(schemaObj: SchemaObj)  : List<Table>{

        val tables = mutableListOf<Table>()

        for (elementInTypes in schemaObj.data?.__schema?.types.orEmpty()) {

            if (elementInTypes.name == "__Schema" ||
                    elementInTypes.name == "__Directive"||
                    elementInTypes.name == "__DirectiveLocation"||
                    elementInTypes.name == "__EnumValue"||
                    elementInTypes.name == "__Field" ||
                    elementInTypes.name == "__InputValue" ||
                    elementInTypes.name == "__Type"||
                    elementInTypes.name == "__TypeKind") {
                break
            }

            /*
                TODO: lot of code here is duplicated, will need to be refactored/simplified
             */

            for (elementInfields in elementInTypes?.fields.orEmpty()) {

                val tableElement = Table()

                tableElement.tableField = elementInfields?.name

                val non_null: __TypeKind? = __TypeKind.NON_NULL

                if (elementInfields?.type?.kind == non_null) {
                    val list: __TypeKind? = __TypeKind.LIST
                    if (elementInfields?.type?.ofType?.kind == list) {
                        tableElement.kindOfTableField = list
                        tableElement.isKindOfKindOfTableFieldOptional = false

                        if (elementInfields?.type?.ofType?.ofType?.kind == non_null) {
                            val obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                                tableElement.tableName = elementInTypes?.name
                                tables.add(tableElement)
                            } else {
                                val scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = false
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                                    tableElement.tableName = elementInTypes?.name
                                    tables.add(tableElement)
                                }

                            }

                        } else {
                            val obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                tableElement.tableName = elementInTypes?.name
                                tables.add(tableElement)

                            } else {
                                val scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = true
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                    tableElement.tableName = elementInTypes?.name
                                    tables.add(tableElement)
                                }

                            }

                        }
                    } else {val obj: __TypeKind? = __TypeKind.OBJECT
                        if (elementInfields?.type?.ofType?.kind == obj){
                            tableElement.kindOfTableType = obj
                            tableElement.isKindOfTableTypeOptional = false
                            tableElement.tableType = elementInfields?.type?.ofType?.name
                            tableElement.tableName = elementInTypes?.name
                            tables.add(tableElement)
                        } else {      val scalar: __TypeKind? = __TypeKind.SCALAR
                            if (elementInfields?.type?.ofType?.kind == scalar){
                                tableElement.kindOfTableType = scalar
                                tableElement.isKindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.name
                                tableElement.tableName = elementInTypes?.name
                                tables.add(tableElement)
                            }

                        }

                    }

                } else {val list: __TypeKind? = __TypeKind.LIST
                    if (elementInfields?.type?.kind == list){
                        tableElement.kindOfTableField = list
                        tableElement.isKindOfKindOfTableFieldOptional = true
                        if (elementInfields?.type?.ofType.kind == non_null ){
                            val obj: __TypeKind? = __TypeKind.OBJECT
                            if ( elementInfields?.type?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                tableElement.tableName = elementInTypes?.name
                                tables.add(tableElement)
                            } else {val scalar: __TypeKind? = __TypeKind.SCALAR
                                if ( elementInfields?.type?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = false
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                    tableElement.tableName = elementInTypes?.name
                                    tables.add(tableElement)
                                }
                            }

                        } else {val obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType.kind == obj ) {
                                tableElement.kindOfTableType = obj
                                tableElement.isKindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.ofType?.name
                                tableElement.tableName = elementInTypes?.name
                                tables.add(tableElement)
                            } else {var scalar: __TypeKind? = __TypeKind.SCALAR
                                if ( elementInfields?.type?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.isKindOfTableTypeOptional = true
                                    tableElement.tableType = elementInfields?.type?.ofType?.name
                                    tableElement.tableName = elementInTypes?.name
                                    tables.add(tableElement)
                                }

                            }

                        }

                    } else{ val obj: __TypeKind? = __TypeKind.OBJECT
                        if (elementInfields?.type?.kind == obj){
                            tableElement.kindOfTableType = obj
                            tableElement.isKindOfTableTypeOptional = true
                            tableElement.tableType = elementInfields?.type?.name
                            tableElement.tableName = elementInTypes?.name
                            tables.add(tableElement)
                        }
                        else{  val scalar: __TypeKind? = __TypeKind.SCALAR
                            if (elementInfields?.type?.kind == scalar){
                                tableElement.kindOfTableType = scalar
                                tableElement.isKindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.name
                                tableElement.tableName = elementInTypes?.name
                                tables.add(tableElement)

                            }

                        }

                    }

                }

            }

        }
        return tables
    }

    private fun handleOperation(
            actionCluster: MutableMap<String, Action>,
            methodName: String?,
            methodType: String?,
            tableType: String,
            kindOfTableType: String,
            kindOfTableField: String?,
            table:List<Table>,
            tableName: String,
            isKindOfTableTypeOptional: Boolean,
            isKindOfkindOfTableFieldOptional : Boolean
    ) {
        if(methodName == null){
            //TODO log warn
            return;
        }
        if(methodType == null){
            //TODO log warn
            return;
        }
        val type = when{
            methodType.equals("QUERY",true) -> GQMethodType.QUERY
            methodType.equals("MUTATION",true) -> GQMethodType.MUTATION
            else -> {
                //TODO log warn
                return
            }
        }

        val actionId = "$methodName${idGenerator.incrementAndGet()}"

        //TODO populate

        val params = extractParams(methodName, tableType, kindOfTableType, kindOfTableField, table, tableName, isKindOfTableTypeOptional, isKindOfkindOfTableFieldOptional)

        val action = GraphQLAction(actionId, methodName, type, params)

        actionCluster[action.getName()] = action
    }
    private fun extractParams(
            methodName: String,
            tableType: String,
            kindOfTableType: String,
            kindOfTableField: String?,
            table: List<Table>,
            tableName: String,
            isKindOfTableTypeOptional: Boolean,
            isKindOfKindOfTableFieldOptional : Boolean
    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque<String>()

        val gene = getGene(tableType, kindOfTableField, kindOfTableType, table, tableName, history,isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional)

            params.add(GQReturnParam(tableType, gene))

        return params
    }

    private fun getGene(
            tableType: String,
            kindOfTableField: String?,
            kindOfTableType: String,
            table: List<Table>,
            tableName: String,
            history: Deque<String> = ArrayDeque<String>(),
            isKindOfTableTypeOptional: Boolean,
            isKindOfKindOfTableFieldOptional : Boolean
    ): Gene {

        when (kindOfTableField?.toLowerCase()) {
            "list" -> {
                if(isKindOfKindOfTableFieldOptional){
                    val template = getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                                            isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional )
                    return OptionalGene(tableName,ArrayGene(tableName, template))

                }else {
                    val template = getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                            isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional )
                    return ArrayGene(tableName, template)
                }
            }
            "object" -> {
                if(isKindOfTableTypeOptional){
                    val optObjGene=createObjectGene(tableName, tableType, kindOfTableType, table, history,
                                                    isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional)
                   return OptionalGene(tableName, optObjGene)
                }else {
                    return createObjectGene(tableName, tableType, kindOfTableType, table, history,
                                            isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                }
            }

            "int" -> { if (isKindOfTableTypeOptional){
                return OptionalGene(tableName,IntegerGene(tableName))
                         }
                    else { return IntegerGene(tableName)
                            }
            }

            "string" -> {if(isKindOfTableTypeOptional){
                return OptionalGene(tableName, StringGene(tableName))
            } else {return StringGene(tableName)}

            }
            "float" -> {if(isKindOfTableTypeOptional){
                return OptionalGene(tableName, FloatGene(tableName))
            } else { return FloatGene(tableName)
            }
            }

            "boolean" -> {if(isKindOfTableTypeOptional){
                return OptionalGene(tableName, BooleanGene(tableName))
            }else {
                return BooleanGene(tableName)}
            }
            "null"-> {if(isKindOfTableTypeOptional){
                val optNonListGene= getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                        isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional )
                return OptionalGene(tableName,optNonListGene)
            }else {
                return getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                        isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional )}
            }
            "date"-> {if(isKindOfTableTypeOptional){return OptionalGene(tableName, BooleanGene(tableName))
            }else {
                return DateGene(tableName)}
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
                                 table: List<Table>,
                                 history: Deque<String> = ArrayDeque<String>(),
                                 isKindOfTableTypeOptional : Boolean,
                                 isKindOfKindOfTableFieldOptional: Boolean
    ): Gene {
        val fields :MutableList<Gene> = mutableListOf()
        for (element in table) {
            if (element.tableName == tableName) {
                if(element.kindOfTableType.toString().equals("SCALAR", ignoreCase = true) ) {
                    val field = element.tableField
                    val template = field?.let {
                        getGene(tableName, element.tableType, kindOfTableType, table, it, history,
                                element.isKindOfTableTypeOptional,isKindOfKindOfTableFieldOptional )
                    }
                    if (template != null) {
                        fields.add(template)
                    }
                }
                else {
                    if (element.kindOfTableField.toString().equals("LIST", ignoreCase = true)) {
                        val template= getGene(element.tableType, element.kindOfTableField.toString(), element.kindOfTableType.toString(),
                                table, element.tableType, history, isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)

                        if (template != null) {
                            fields.add(template)
                        }
                    } else {
                        if (element.kindOfTableType.toString().equals("OBJECT", ignoreCase = true)) {
                            history.add(element.tableName)
                            if (history.count { it == element.tableName } == 1) {
                                val template = getGene(element.tableType, element.kindOfTableType.toString(), element.kindOfTableField.toString(),
                                        table, element.tableType, history, isKindOfTableTypeOptional, isKindOfKindOfTableFieldOptional)
                                if (template != null) {
                                    fields.add(template)
                                }
                            }else {
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