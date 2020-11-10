package org.evomaster.core.problem.graphql.schema

/**
 * FullType: fragment  on __Type
 */
data class FullType( var kind: __TypeKind,
                     var name: String,
                     var fields : ArrayList<__Field> = ArrayList(),
                     var inputFields : ArrayList<InputValue> = ArrayList(),
                     var interfaces : ArrayList<TypeRef> = ArrayList(),
                     var enumValues : ArrayList<__EnumValue> = ArrayList(),
                     var possibleTypes : ArrayList<TypeRef> = ArrayList()) {

}