package org.evomaster.core.problem.graphql.builder

import org.evomaster.core.problem.graphql.schema.__TypeKind

/**
 * Intermediate data structure to parse and organize the object graphQl-schema types.
 *
 * WARN: this class MUST be kept immutable
 * */
data class Table(
        /**
         * Specify the name of the table type.
         * For example, Query in petclinic.graphqls
         */
        val typeName: String,
        /**
         * Specify the name of the field in the table.
         * for example pettypes (inside Query) in petclinic.graphqls.
         * Ie. the name of field in a node.
         *
         * This must always be present.
         */
        val fieldName: String,
        /**
         * Describing the kind of the tableField, eg: LIST
         */
        val kindOfTableField: __TypeKind = __TypeKind.NULL, // TODO check this NULL
        /**
         * Describing if the kind of the table field is nullable
         */
        val isKindOfTableFieldOptional: Boolean = false,
        /**
         * Describing the type of the table field.
         * For example, the name of the object representing this type.
         */
        val tableFieldType: String = "",
        /**
         * Describing the kind of the tableFieldType, eg: SCALAR, OBJECT,INPUT_OBJECT, ENUM
         */
        val kindOfTableFieldType: __TypeKind,
        /**
         * Describing if the kind of the table field type is nullable
         */
        val isKindOfTableFieldTypeOptional: Boolean = false,
        /**
         * Describing if the table field has arguments
         */
        val tableFieldWithArgs: Boolean = false,

        /*
         * Containing the enum values
         */
        val enumValues: List<String> = listOf(),
        /*
         * Containing the union possible types
         */
        val unionTypes: List<String> = listOf(),
        /*
         * Containing the interface possible types
          */
        val interfaceTypes: List<String> = listOf()

){

        val uniqueId = if(typeName == null)  fieldName else "$typeName.$fieldName"

}