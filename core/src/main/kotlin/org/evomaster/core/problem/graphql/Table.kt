package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.schema.__TypeKind

/**
 * Intermediate structure to parse and organize the object graphQl-schema types into five components: Name, Field,
 * kindOfTableField, Type, kindOfTableType
 * */
class Table {
    var tableField : String?=null
    var tableType : String= ""
    var kindOfTableField: __TypeKind?=null
    var tableName : String?=null
    var kindOfTableType: __TypeKind?=null
}