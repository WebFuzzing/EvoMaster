package org.evomaster.core.problem.graphql.builder

data class TempState(
        /**
         * A data structure used to store information extracted from the schema, i.e., all Objects types,
         * including not only queries/mutations, but also all other object definitions, as well as all
         * interfaces and unions
         */
        var tables: MutableList<Table> = mutableListOf(),

        var tablesIndexedByName : MutableMap<String, List<Table>> = mutableMapOf(),

        /**
         * A data structure used to store information extracted from the schema about input types eg, Input types.
         * Those are the input parameters to the functions (eg, in top-level queries/mutations, as well as nested
         * functions in object definitions)
         */
        var argsTables: MutableList<Table> = mutableListOf(),

        var argsTablesIndexedByName : MutableMap<String, List<Table>> = mutableMapOf(),
        /*
        * An intermediate data structure used for extracting argsTables
       */
        val tempArgsTables: MutableList<Table> = mutableListOf(),
        /*
         * An intermediate data structure used for extracting Union types
         */
        var tempUnionTables: MutableList<Table> = mutableListOf(),

        /*
         * A map used to link each function name to a unique ID.
         * @Key: unique ID
         * @Value: function name
         */
        var inputTypeName: MutableMap<String,String> = mutableMapOf()
)