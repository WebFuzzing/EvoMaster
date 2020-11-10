package org.evomaster.core.problem.graphql.schema

/**
 *  Field: __directive part of the introspection system.
 */
data class __Directive( var name: String,
                        var locations: ArrayList<__DirectiveLocation> = ArrayList(),
                        var args: ArrayList<InputValue> = ArrayList()) {


}