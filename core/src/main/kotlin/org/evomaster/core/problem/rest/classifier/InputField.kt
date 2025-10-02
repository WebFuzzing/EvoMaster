package org.evomaster.core.problem.rest.classifier

enum class InputFieldType{
    QUERY, BODY
}


/**
 * Represent a field in the input of a REST call, for which are aiming to learn its constraints
 */
data class InputField(
    val name : String,
    val type : InputFieldType
)

