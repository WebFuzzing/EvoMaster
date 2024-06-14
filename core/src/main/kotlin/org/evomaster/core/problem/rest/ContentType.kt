package org.evomaster.core.problem.rest

/**
 * Created by arcuri82 on 24-Oct-19.
 */
enum class ContentType(
    val defaultValue : String
) {

    JSON("application/json"),
    X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded");



    companion object {

        fun from(s : String) : ContentType{
            return if(s.trim().endsWith("json", true)){
                JSON
            } else if(s.trim().endsWith("x-www-form-urlencoded", true)){
                X_WWW_FORM_URLENCODED
            } else {
                throw IllegalArgumentException("Not able to handle content type: $s")
            }
        }
    }
}