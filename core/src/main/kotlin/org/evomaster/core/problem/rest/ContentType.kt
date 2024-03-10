package org.evomaster.core.problem.rest

/**
 * Created by arcuri82 on 24-Oct-19.
 */
enum class ContentType {

    JSON,
    X_WWW_FORM_URLENCODED;

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