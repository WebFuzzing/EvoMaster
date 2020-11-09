package org.evomaster.core.problem.graphql.schema

class __Directive {

     var name: String?=null
     var locations= ArrayList<__DirectiveLocation?>()
     var args= ArrayList<InputValue?>()


    override fun toString(): String {
        return "{name: ${this.name}, locations: ${this.locations}, args: ${this.args}}"

    }


}
