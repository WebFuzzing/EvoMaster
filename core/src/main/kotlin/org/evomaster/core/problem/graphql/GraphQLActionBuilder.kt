package org.evomaster.core.problem.graphql

import ch.qos.logback.classic.db.names.TableName
import com.google.gson.Gson
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.search.Action
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
            - create action for it which the needed genes// asma: Not yet
            - add the action to actionCluster
         */

        var table : MutableList<Table> = mutableListOf()


        for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {

            if (elementIntypes.name == "__Schema") {
                break
            }
            for (elementInfields in elementIntypes?.fields.orEmpty()) {

                var tableElement = Table()

                tableElement.tableField = elementInfields?.name

                while (elementInfields?.type?.ofType?.name == null) {

                    elementInfields?.type?.ofType = elementInfields?.type?.ofType?.ofType

                }

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
                handleOperation(actionCluster, elementIntable?.tableName, elementIntable?.tableField, elementIntable?.tableType)
            }
        }

    }

    private fun handleOperation(
            actionCluster: MutableMap<String, Action>,
            tableName: String?,
            tableField: String?,
            tableType: String?
    ) {


        val actionId = "$tableName$tableField${idGenerator.incrementAndGet()}"
        val action = GraphQLAction(actionId, tableName, tableField, tableType)

        actionCluster[action.getName()] = action
    }


}