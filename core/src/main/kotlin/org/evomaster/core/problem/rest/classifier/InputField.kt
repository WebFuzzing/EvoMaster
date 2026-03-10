package org.evomaster.core.problem.rest.classifier

enum class InputFieldType{
    QUERY, BODY
}


/**
 * Represent a field in the input of a REST call, for which are aiming to learn its constraints
 */
data class InputField(
    /**
     *  Eg, name of query parameter, or field in body payload.
     *  For nested fields, used '.', eg, "address.city"
     */
    val name : String,
    val type : InputFieldType
){
    fun isWholeBody() = type == InputFieldType.BODY && name.isEmpty()

}

