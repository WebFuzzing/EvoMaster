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


        for (elementIntable in tables) {

            if (elementIntable.tableName == "Mutation" || elementIntable.tableName == "Query") {

                handleOperation(actionCluster,
                        elementIntable.tableField,
                        elementIntable.tableName,
                        elementIntable.tableType,
                        elementIntable.kindOfTableType.toString(),
                        elementIntable.kindOfTableField.toString(),
                        tables,
                        elementIntable.tableName.toString(),
                        elementIntable.IskindOfTableTypeOptional,
                        elementIntable.IskindOfkindOfTableFieldOptional)

            }
        }

    }

    private fun initTableInfo(schemaObj: SchemaObj)  : List<Table>{

        val tables = mutableListOf<Table>()

        for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {

            if (elementIntypes.name == "__Schema" ||
                    elementIntypes.name == "__Directive"||
                    elementIntypes.name == "__DirectiveLocation"||
                    elementIntypes.name == "__EnumValue"||
                    elementIntypes.name == "__Field" ||
                    elementIntypes.name == "__InputValue" ||
                    elementIntypes.name == "__Type"||
                    elementIntypes.name == "__TypeKind") {
                break
            }

            for (elementInfields in elementIntypes?.fields.orEmpty()) {

                var tableElement = Table()

                tableElement.tableField = elementInfields?.name

                var non_null: __TypeKind? = __TypeKind.NON_NULL

                if (elementInfields?.type?.kind == non_null) {
                    var list: __TypeKind? = __TypeKind.LIST
                    if (elementInfields?.type?.ofType?.kind == list) {
                        tableElement.kindOfTableField = list
                        tableElement.IskindOfkindOfTableFieldOptional = false

                        if (elementInfields?.type?.ofType?.ofType?.kind == non_null) {
                            var obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.IskindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            } else {
                                var scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.IskindOfTableTypeOptional = false
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }

                            }

                        } else {
                            var obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.IskindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)

                            } else {
                                var scalar: __TypeKind? = __TypeKind.SCALAR
                                if (elementInfields?.type?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.IskindOfTableTypeOptional = true
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }

                            }

                        }
                    } else {var obj: __TypeKind? = __TypeKind.OBJECT
                        if (elementInfields?.type?.ofType?.kind == obj){
                            tableElement.kindOfTableType = obj
                            tableElement.IskindOfTableTypeOptional = false
                            tableElement.tableType = elementInfields?.type?.ofType?.name
                            tableElement.tableName = elementIntypes?.name
                            tables.add(tableElement)
                        } else {      var scalar: __TypeKind? = __TypeKind.SCALAR
                            if (elementInfields?.type?.ofType?.kind == scalar){
                                tableElement.kindOfTableType = scalar
                                tableElement.IskindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            }

                        }

                    }

                } else {var list: __TypeKind? = __TypeKind.LIST
                    if (elementInfields?.type?.kind == list){
                        tableElement.kindOfTableField = list
                        tableElement.IskindOfkindOfTableFieldOptional = true
                        if (elementInfields?.type?.ofType.kind == non_null ){
                            var obj: __TypeKind? = __TypeKind.OBJECT
                            if ( elementInfields?.type?.ofType?.ofType?.kind == obj) {
                                tableElement.kindOfTableType = obj
                                tableElement.IskindOfTableTypeOptional = false
                                tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            } else {var scalar: __TypeKind? = __TypeKind.SCALAR
                                if ( elementInfields?.type?.ofType?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.IskindOfTableTypeOptional = false
                                    tableElement.tableType = elementInfields?.type?.ofType?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }
                            }

                        } else {var obj: __TypeKind? = __TypeKind.OBJECT
                            if (elementInfields?.type?.ofType.kind == obj ) {
                                tableElement.kindOfTableType = obj
                                tableElement.IskindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.ofType?.name
                                tableElement.tableName = elementIntypes?.name
                                tables.add(tableElement)
                            } else {var scalar: __TypeKind? = __TypeKind.SCALAR
                                if ( elementInfields?.type?.ofType?.kind == scalar) {
                                    tableElement.kindOfTableType = scalar
                                    tableElement.IskindOfTableTypeOptional = true
                                    tableElement.tableType = elementInfields?.type?.ofType?.name
                                    tableElement.tableName = elementIntypes?.name
                                    tables.add(tableElement)
                                }

                            }

                        }

                    } else{ var obj: __TypeKind? = __TypeKind.OBJECT
                        if (elementInfields?.type?.kind == obj){
                            tableElement.kindOfTableType = obj
                            tableElement.IskindOfTableTypeOptional = true
                            tableElement.tableType = elementInfields?.type?.name
                            tableElement.tableName = elementIntypes?.name
                            tables.add(tableElement)
                        }
                        else{  var scalar: __TypeKind? = __TypeKind.SCALAR
                            if (elementInfields?.type?.kind == scalar){
                                tableElement.kindOfTableType = scalar
                                tableElement.IskindOfTableTypeOptional = true
                                tableElement.tableType = elementInfields?.type?.name
                                tableElement.tableName = elementIntypes?.name
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
            IskindOfTableTypeOptional: Boolean,
            IskindOfkindOfTableFieldOptional : Boolean
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

        val params = extractParams(methodName, tableType, kindOfTableType, kindOfTableField, table, tableName, IskindOfTableTypeOptional, IskindOfkindOfTableFieldOptional)

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
            IskindOfTableTypeOptional: Boolean,
            IskindOfkindOfTableFieldOptional : Boolean
    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque<String>()

        val gene = getGene(tableType, kindOfTableField, kindOfTableType, table, tableName, history,IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional)

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
            IskindOfTableTypeOptional: Boolean,
            IskindOfkindOfTableFieldOptional : Boolean
    ): Gene {

        when (kindOfTableField?.toLowerCase()) {
            "list" -> {
                if(IskindOfkindOfTableFieldOptional== true){
                    val template = getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                                            IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional )
                    return OptionalGene(tableName,ArrayGene(tableName, template))

                }else {
                    val template = getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                            IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional )
                    return ArrayGene(tableName, template)
                }
            }
            "object" -> {
                if(IskindOfTableTypeOptional== true){
                    val optObjGene=createObjectGene(tableName, tableType, kindOfTableType, table, history,
                                                    IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional)
                   return OptionalGene(tableName, optObjGene)
                }else {
                    return createObjectGene(tableName, tableType, kindOfTableType, table, history,
                                            IskindOfTableTypeOptional, IskindOfkindOfTableFieldOptional)
                }
            }

            "int" -> { if (IskindOfTableTypeOptional == true){
                return OptionalGene(tableName,IntegerGene(tableName))
                         }
                    else { return IntegerGene(tableName)
                            }
            }

            "string" -> {if(IskindOfTableTypeOptional == true){
                return OptionalGene(tableName, StringGene(tableName))
            } else {return StringGene(tableName)}

            }
            "float" -> {if(IskindOfTableTypeOptional== true){
                return OptionalGene(tableName, FloatGene(tableName))
            } else { return FloatGene(tableName)
            }
            }

            "boolean" -> {if(IskindOfTableTypeOptional== true){
                return OptionalGene(tableName, BooleanGene(tableName))
            }else {
                return BooleanGene(tableName)}
            }
            "null"-> {if(IskindOfTableTypeOptional== true){
                val optNonListGene= getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                        IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional )
                return OptionalGene(tableName,optNonListGene)
            }else {
                return getGene(tableName, kindOfTableType, kindOfTableField, table, tableType, history,
                        IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional )}
            }
            "date"-> {if(IskindOfTableTypeOptional== true){return OptionalGene(tableName, BooleanGene(tableName))
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
                                 IskindOfTableTypeOptional : Boolean,
                                 IskindOfkindOfTableFieldOptional: Boolean
    ): Gene {
        val fields :MutableList<Gene> = mutableListOf()
        for (element in table) {
            if (element.tableName == tableName) {
                if(element.kindOfTableType.toString().equals("SCALAR", ignoreCase = true) ) {
                    val field = element.tableField
                    val template = field?.let {
                        getGene(tableName, element.tableType, kindOfTableType, table, it, history,
                                element.IskindOfTableTypeOptional,IskindOfkindOfTableFieldOptional )
                    }
                    if (template != null) {
                        fields.add(template)
                    }
                }
                else {
                    if (element.kindOfTableField.toString().equals("LIST", ignoreCase = true)) {
                        val template= getGene(element.tableType, element.kindOfTableField.toString(), element.kindOfTableType.toString(),
                                table, element.tableType, history, IskindOfTableTypeOptional, IskindOfkindOfTableFieldOptional)

                        if (template != null) {
                            fields.add(template)
                        }
                    } else {
                        if (element.kindOfTableType.toString().equals("OBJECT", ignoreCase = true)) {
                            history.add(element.tableName)
                            if (history.count { it == element.tableName } == 1) {
                                val template = getGene(element.tableType, element.kindOfTableType.toString(), element.kindOfTableField.toString(),
                                        table, element.tableType, history, IskindOfTableTypeOptional, IskindOfkindOfTableFieldOptional)
                                if (template != null) {
                                    fields.add(template)
                                }
                            }
                        }
                    }
                }
            }

        }
        return ObjectGene(tableName, fields, tableName)
    }

}