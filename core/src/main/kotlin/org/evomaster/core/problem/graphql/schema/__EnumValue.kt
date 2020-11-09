package org.evomaster.core.problem.graphql.schema


class __EnumValue {

     var name:String?=null
     var isDeprecated: Boolean= true
     var deprecationReason: String?=null

    override fun toString(): String {
        return "{ name: ${this.name}, isDeprecated: ${this.isDeprecated}, deprecationReason: ${this.deprecationReason }"

    }

}
