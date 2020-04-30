package org.evomaster.resource.rest.generator.model

/**
 * created by manzh on 2019-08-15
 */
enum class RestMethod(val text : String) {
    POST("POST"),
    POST_VALUE("POST"),
    POST_ID("POST"),
    PUT("PUT"),
    //PUT_VALUE,
    GET_ID("GET"),
    GET_ALL("GET"),
    GET_ALL_CON("GET"), // get all resoruces with constraints on its owners by their ids
    //PATCH,
    PATCH_VALUE("PATCH"),
    DELETE("DELETE"),
    DELETE_CON("DELETE")
}