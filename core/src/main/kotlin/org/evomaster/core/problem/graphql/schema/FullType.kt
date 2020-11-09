package org.evomaster.core.problem.graphql.schema


class FullType {

     var kind: __TypeKind?=null
     var name: String?= null
     var fields = ArrayList<__Field?>()
     var inputFields = ArrayList<InputValue?>()
     var interfaces = ArrayList<TypeRef?>()
     var enumValues = ArrayList<__EnumValue?>()
     var possibleTypes = ArrayList<TypeRef?>()

    override fun toString(): String {
        return "{ kind: ${this.kind}, name: ${this.name}, fields ${this.fields}, inputFields: ${this.inputFields}, interfaces: ${this.interfaces}," +
                " enumValues: ${this.enumValues}, possibleTypes: ${this.possibleTypes}}"

    }

}