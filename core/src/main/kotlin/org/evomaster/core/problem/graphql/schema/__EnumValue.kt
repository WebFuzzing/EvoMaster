package org.evomaster.core.problem.graphql.schema

/**
 *  Field: __enumValue part of the introspection system.
 */
class __EnumValue {

     var name:String?=null
     var isDeprecated: Boolean= true
     var deprecationReason: String?=null

    override fun toString(): String {
        return "{ name: ${this.name}, isDeprecated: ${this.isDeprecated}, deprecationReason: ${this.deprecationReason }"

    }

}
