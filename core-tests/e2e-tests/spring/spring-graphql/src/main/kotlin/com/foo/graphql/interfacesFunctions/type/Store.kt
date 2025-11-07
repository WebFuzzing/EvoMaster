package com.foo.graphql.interfacesFunctions.type


interface Store {

    val id: Int?
    val name: String?


    fun name(x: Int): String

    fun findNameByX(x: Int): String 

}