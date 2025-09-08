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

/**
 * Type of encoding a double vector
 * RAW with no change
 * NORMAL as a standard normalized vector
 * UNIT_NORMAL convert the vector to a unit vector as a member of a unit sphere
 */
enum class EncoderType {
    RAW, NORMAL, UNIT_NORMAL
}

