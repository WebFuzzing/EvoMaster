package org.evomaster.core.problem.graphQl.schema

/**
 *  Field: __field part of the introspection system.
 */

data class __Field (var name: String,
                    var args : ArrayList<InputValue> = ArrayList(),
                    var type : TypeRef,
                    var isDeprecated: Boolean= true,
                    var deprecationReason: String){


}
