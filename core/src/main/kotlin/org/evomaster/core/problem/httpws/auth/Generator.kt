package org.evomaster.core.problem.httpws.auth

import com.webfuzzing.commons.auth.Generator

class Generator(

    /**
     * Placeholder tag used to represent a value generated with this generator. \
     * String interpolation will be applied to the raw payloads to replace any found instance of \
     * this placeholder with the generated value.
     */
    val placeHolder: String,

    /**
     * Minimum length of the generated string
     */
    val minLength: Int?,

    /**
     * Maximum length of the generated string
     */
    val maxLength: Int?,

    /**
     * Fixed prefix shared by all generated strings
     */
    val prefix: String?,

    /**
     * Fixed postfix shared by all generated strings
     */
    val postfix: String?
){

    init{
        if(placeHolder.isEmpty()){
            throw IllegalArgumentException("Placeholder can not be empty")
        }
        if(minLength != null && minLength < 0){
            throw IllegalArgumentException("Minimum length must be greater than or equal to 0, but was $minLength")
        }
        if(maxLength != null && maxLength < 0){
            throw IllegalArgumentException("Maximum length must be greater than or equal to 0, but was $maxLength")
        }

        if(maxLength != null) {
            var length = 0
            if (prefix != null) {
                length += prefix.length
            }
            if (postfix != null) {
                length += postfix.length
            }
            if (length >= maxLength) {
                throw IllegalArgumentException("If specified, maximum length must be greater than prefix+postfix")
            }
        }
    }


    companion object {

        fun fromDto(dto: Generator) : org.evomaster.core.problem.httpws.auth.Generator{

            return Generator(
                placeHolder = dto.placeHolder,
                minLength = dto.minLength,
                maxLength = dto.maxLength,
                prefix = dto.prefix,
                postfix = dto.postfix
            )
        }

    }

}