package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.schema.__TypeKind

/**
 * Intermediate data structure to parse and organize the object graphQl-schema types
 * */
class Table(
    var tableField : String?=null,
    /**
     * For example integer, object, list, etc.
     */
    var tableType : String= "",
    /**
     * describing what kind of type the the table field is
     */
    var kindOfTableField: __TypeKind?=null,
    var tableName : String?=null,
    /**
     * Describing what kind of sub-type, eg, if this is a list, then specify
     * the type for the list elements
     */
    var kindOfTableType: __TypeKind?=null
)