package org.evomaster.core.problem.graphql.schema

/**
 * FullType: fragment  on __Type
 */
data class FullType( var kind: __TypeKind,
                     var name: String,
                     var fields : MutableList<__Field>? = mutableListOf(),
                     var inputFields : MutableList<InputValue> = mutableListOf(),
                     var interfaces : MutableList<TypeRef> = mutableListOf(),
                     var enumValues : MutableList<__EnumValue> = mutableListOf(),
                     var possibleTypes : MutableList<TypeRef> = mutableListOf()) {

}