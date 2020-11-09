package org.evomaster.core.problem.graphql.schema

class __Type {

     var name: String?= null
     var kind: __TypeKind?=null


    override fun toString(): String {
        return "{ name: ${this.name}, kind: ${this.kind}, }"

    }
}
