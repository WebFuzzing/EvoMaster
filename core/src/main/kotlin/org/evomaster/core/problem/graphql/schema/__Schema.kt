package org.evomaster.core.problem.graphql.schema

/**
 *  Field: __schema part of the introspection system.
 */
class __Schema {

     var queryType: __Type?= null
     var mutationType: __Type?= null
     var types = ArrayList<FullType?>()
     var directives = ArrayList<__Directive?>()




   override fun toString(): String {
        return "__Schema { queryType ${this.queryType}, mutationType ${this.mutationType}, types ${this.types}, directives: ${this.directives}}"

    }

}
