package org.evomaster.core.problem.graphql.schema

class TypeRef {

     var kind: __TypeKind?=null
     var name: String?= null
     var ofType: ofTypeOn__Type?=null



    override fun toString(): String {
        return "{ kind: ${this.kind}, name: ${this.name}, ofType: ${this.ofType} }"

    }



}


