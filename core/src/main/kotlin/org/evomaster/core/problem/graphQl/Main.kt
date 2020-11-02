import com.google.gson.Gson
import java.io.File
import java.io.InputStream

class Main {


    fun main(args: Array<String>) {

        //reading the gsonfile
        val inputStream: InputStream = File("src/main/resources/QuerryTypeGlobalPetsClinic.json").inputStream()
        val json = inputStream.bufferedReader().use { it.readText() }
        //println(json)

        //converting json to object
        val gson = Gson()
        val schemaObj: SchemaObj = gson.fromJson(json, SchemaObj::class.java)
        println("From JSON To OBJECT:\n" + schemaObj)


        var actionCluster_list = ArrayList<ActionCluster?>() // not yet used


        for (elementIntypes in schemaObj.data?.__schema?.types.orEmpty()) {

            var table = ArrayList<Table?>()


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

            for (elementIntable in table) {
                println("{Table Name: ${elementIntable?.tableName}, Field: ${elementIntable?.tableField}, Type: ${elementIntable?.tableType}}")

            }


        }
    }



}