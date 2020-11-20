package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.problem.graphql.schema.__TypeKind
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import java.util.concurrent.atomic.AtomicInteger


object GraphQLActionBuilder {

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

        var action : MutableList<GraphQLAction> = mutableListOf()
        var table: MutableList<Table> = mutableListOf()


        for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {

            if (elementIntypes.name == "__Schema") {
                break
            }
            for (elementInfields in elementIntypes?.fields.orEmpty()) {

                var tableElement = Table()

                tableElement.tableField = elementInfields?.name

                var list: __TypeKind? = __TypeKind.LIST

                if (elementInfields?.type?.ofType?.kind == list){


                    tableElement.kindOfTableField =elementInfields?.type?.ofType?.kind
                }

                while (elementInfields?.type?.ofType?.name == null) {

                    elementInfields?.type?.ofType = elementInfields?.type?.ofType?.ofType

                }

                tableElement.kindOfTableType= elementInfields?.type?.ofType?.kind
                tableElement.tableType = elementInfields?.type?.ofType?.name
                tableElement.tableName = elementIntypes?.name
                table.add(tableElement)

            }


            for (elementIntable in table) {
                if (elementIntable?.tableField == elementIntypes?.name) {
                    var tableElement = Table()
                    for (elementInfields in elementIntypes?.fields.orEmpty()) {
                        tableElement.tableField = elementInfields?.name
                        while (elementInfields?.type?.ofType?.name == null) {

                            elementInfields?.type?.ofType = elementInfields?.type?.ofType?.ofType

                        }
                        tableElement.tableType = elementInfields?.type?.ofType?.name
                        tableElement.tableName = elementIntypes?.name
                        table.add(tableElement)
                    }


                }

            }


        }


        for (elementIntable in table) {

            if (elementIntable.tableName == "Mutation" || elementIntable.tableName == "Query") {

                handleOperation(actionCluster, elementIntable.tableField, elementIntable.tableName,
                        elementIntable.tableType,
                        elementIntable.kindOfTableType.toString(),
                        elementIntable.kindOfTableField.toString(),
                        table,
                        elementIntable.tableName.toString())

            }
        }

    }
    private fun handleOperation(
            actionCluster: MutableMap<String, Action>,
            methodName: String?,
            methodType: String?,
            tableType: String,
            kindOfTableType: String,
            kindOfTableField: String,
            table:MutableList<Table>,
            tableName: String
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

        val params = extractParams(methodName, methodType, tableType, kindOfTableType, kindOfTableField, table, tableName)

        val action = GraphQLAction(actionId, methodName, type, params)

        actionCluster[action.getName()] = action
    }
    private fun extractParams(
            methodName: String,
            methodType: String,
            tableType: String,
            kindOfTableType: String,
            kindOfTableField: String,
            table: MutableList<Table>,
            tableName: String

    ): MutableList<Param> {

        val params = mutableListOf<Param>()

        var gene = getGene(tableType, kindOfTableField, kindOfTableType, table, tableName)

        params.add(GQReturnParam(tableType, gene))


        return params
    }

    private fun getGene(
            tableType: String,
            kindOfTableField: String,
            kindOfTableType: String,
            table: MutableList<Table>,
            tableName: String
    ): Gene {

        when (kindOfTableField) {
            "LIST" -> {
                var template = getGene(tableName, kindOfTableType, kindOfTableField, table, tableType)
                return ArrayGene(tableName, template)
            }
            "OBJECT" -> {
                return createObjectGene(tableName, tableType, kindOfTableType, table)
            }

            "Int" -> {
                return IntegerGene(tableName)
            }

            "String" -> {
                return StringGene(tableName)
            }
            else -> {
                return StringGene("NotFound")
            }
        }

    }
    private fun createObjectGene(tableName: String,
                                 tableType: String,
                                 kindOfTableType: String,
                                 table: MutableList<Table>): Gene {
        var fields :MutableList<Gene> = mutableListOf()
        for (element in table) {
            if (element.tableName == tableName) {
                var field = element.tableField
                var template = field?.let { getGene(tableName, element.tableType,kindOfTableType, table, it) }
                if (template != null) {
                    fields.add(template)
                }
            }

        }
        return ObjectGene(tableName, fields, tableName)
    }

}