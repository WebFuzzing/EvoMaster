package org.evomaster.core.problem.graphql.schema

/**
 *  Field: __directive part of the introspection system.
 */
data class __Directive( var name: String,
                        var locations: MutableList<__DirectiveLocation> = mutableListOf(),
                        var args: MutableList<InputValue> = mutableListOf()) {


}

