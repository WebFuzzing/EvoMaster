package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.template.Tag

/**
 * created by manzh on 2019-08-15
 */
object SwaggerAnnotation {

    /**
     * Provides additional information about Swagger models.
     */
    val API_MODEL = Tag("ApiModel")

    /**
     * Adds and manipulates data of a model property.
     */
    val API_MODEL_PROPERTY = object : Tag("ApiModelProperty"){

        /**
         * valid params:
         * readOnly : boolean -- Allows a model property to be designated as read only.
         * required : boolean -- Specifies if the parameter is required or not.
         * value : String -- A brief description of this property.
         * hide : boolean -- Allows a model property to be hidden in the Swagger model definition.
         * example : String -- A sample value for the property.
         * dataType : String -- The data type of the parameter.
         */
        private val params = mapOf("readOnly" to true, "required" to true, "value" to false, "hide" to true, "example" to false, "dataType" to false)

        override fun validateParams(param: String): Boolean = params.containsKey(param)

        override fun withoutQuotation(param: String): Boolean = params.getValue(param)
    }

    val ENABLE_SWAGGER_2 = Tag("EnableSwagger2")

    fun requiredPackages() : Array<String> = arrayOf(
            "io.swagger.annotations.ApiModel",
            "io.swagger.annotations.ApiModelProperty"
    )
}