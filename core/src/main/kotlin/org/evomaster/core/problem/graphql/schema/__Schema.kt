package org.evomaster.core.problem.graphql.schema

/**
 *  Field: __schema part of the introspection system.
 */
data class __Schema (var queryType: __Type,
                     var mutationType: __Type,
                     var types :  ArrayList<FullType> = ArrayList(),
                     var directives : ArrayList<__Directive> = ArrayList()){


}