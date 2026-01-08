package com.foo.rest.examples.spring.openapi.v3.expectations

open class GenericObject

open class ExampleObject(
    var ident: Int? = null,
    var name : String? = "Unnamed",
    var description : String? = "Indescribable"
) : GenericObject() {
    fun setId(id: Int? = null){
        ident = id
    }
    fun getId() : Int{
       return when (ident) {
           null -> 0
           else -> ident!!
       }
    }
}

open class OtherExampleObject(
        var id : Int? = null,
        var namn : String? = "Unnamed",
        var category : String = "None"
): GenericObject()
