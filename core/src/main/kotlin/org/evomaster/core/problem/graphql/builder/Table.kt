package org.evomaster.core.problem.graphql.builder

/**
 * Intermediate data structure to parse and organize the object graphQl-schema types
 * WARN: this class MUST be kept immutable
 * */
data class Table(
        /**
         * Specify the name of the type.
         * For example, in petclinic.graphqls:
         * typeName: Query
         */
        val typeName: String,

        /**
         * Specify the name of the field. I.e. the name of field in a node
         * For example: pettypes (inside Query) in petclinic.graphqls.
         */
        val fieldName: String,

        /**
         * Describing the kind of the field name
         * For example: pettypes is a LIST
         */
        val KindOfFieldName: String = "null",

        /**
         * Describing if the kind of the field name is nullable
         * isKindOfFieldNameOptional: false
         */
        val isKindOfFieldNameOptional: Boolean = false,

        /**
         * Describing the type of the field
         * For example: PetType,
         */
        val fieldType: String,

        /**
         * Describing the kind of the Field Type, eg: SCALAR, OBJECT,INPUT_OBJECT, ENUM
         * For example: PetType is an OBJECT
         */
        val kindOfFieldType: String,

        /**
         * Describing if the kind of the table field type is nullable
         * For example: the OBJECT: PetType is not optional
         */
        val isKindOfFieldTypeOptional: Boolean = false,

        /**
         * Describing if the field name has arguments
         */
        val isFieldNameWithArgs: Boolean = false,

        /**
         * Containing the enum values
         */
        val enumValues: List<String> = listOf(),

        /**
         * Containing the union possible types
         */
        val unionTypes: List<String> = listOf(),

        /**
         * Containing the interface possible types
          */
        val interfaceTypes: List<String> = listOf()

){

        val uniqueId = "$typeName.$fieldName.$fieldType"

}