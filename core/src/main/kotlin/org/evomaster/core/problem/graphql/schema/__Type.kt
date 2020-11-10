package org.evomaster.core.problem.graphql.schema

/**
 *  Field: __type part of the introspection system.
 */
class __Type {

     var name: String?= null
     var kind: __TypeKind?=null


    override fun toString(): String {
        return "{ name: ${this.name}, kind: ${this.kind}, }"

    }
}
