package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.template.Tag

/**
 * created by manzh on 2019-08-15
 */
object SpringAnnotation {

    //from org.springframework.web.bind.annotation
    /**
     * A convenience annotation that is itself annotated with @Controller and @ResponseBody.
     */
    val REST_CONTROLLER = Tag("RestController")

    /**
     * Annotation for mapping web requests onto methods in request-handling classes with flexible method signatures.
     */
    val REQUEST_MAPPING = object : Tag("RequestMapping"){

        /**
         * valid params:
         * consumes : String[] -- The consumable media types of the mapped request, narrowing the primary mapping.
         * headers :  String[] -- The headers of the mapped request, narrowing the primary mapping.
         * method :  String[] -- The HTTP request methods to map to, narrowing the primary mapping: GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
         * name :  String[] -- Assign a name to this mapping.
         * params :  String[] -- The parameters of the mapped request, narrowing the primary mapping.
         * path :  String[] -- The path mapping URIs (e.g.
         * produces :  String[] -- The producible media types of the mapped request, narrowing the primary mapping.
         * value :  String[] -- The primary mapping expressed by this annotation.
         */
        private val params = mapOf("consumes" to true,"headers" to false,"method" to true,"name" to false,"params" to false,"path" to false,"produces" to true, "value" to false)
        override fun validateParams(param: String): Boolean = params.containsKey(param)
        override fun withoutQuotation(param: String): Boolean = params.getValue(param)
    }
    /**
     * Annotation indicating a method parameter should be bound to the body of the web request.
     */
    val REQUEST_BODY = Tag("RequestBody")

    /**
     * Annotation which indicates that a method parameter should be bound to a web request parameter.
     */
    val REQUEST_PARAM = object : Tag("RequestParam"){
        /**
         * valid params:
         * name : String -- The name of the request parameter to bind to.
         * required :  boolean -- Whether the parameter is required.
         * value :  String -- Alias for name().
         * defaultValue : String The default value to use as a fallback when the request parameter is not provided or has an empty value.
         */
        private val params = mapOf("name" to false,"required" to true, "value" to false, "defaultValue" to false)
        override fun validateParams(param: String): Boolean = params.containsKey(param)
        override fun withoutQuotation(param: String): Boolean = params.getValue(param)

    }

    /**
     * Annotation which indicates that a method parameter should be bound to a URI template variable.
     */
    val PATH_VAR = object : Tag("PathVariable"){
        /**
         * valid params:
         * name : String -- The name of the path variable to bind to.
         * required :  boolean -- Whether the path variable is required.
         * value :  String -- Alias for name().
         */
        private val params = mapOf("name" to false,"required" to true, "value" to false)
        override fun validateParams(param: String): Boolean = params.containsKey(param)
        override fun withoutQuotation(param: String): Boolean = params.getValue(param)

    }

    //org.springframework.beans.factory.annotation.Autowired

    /**
     * Marks a constructor, field, setter method, or config method as to be autowired by Spring's dependency injection facilities.
     */
    val AUTO_WIRED = Tag("Autowired")

    fun requiredPackages() : Array<String> = arrayOf(
            "org.springframework.web.bind.annotation.*",
            "org.springframework.beans.factory.annotation.*"
    )

    //org.springframework.stereotype.Repository

    /**
     * Indicates that an annotated class is a "Repository", originally defined by Domain-Driven Design (Evans, 2003) as "a mechanism for encapsulating storage, retrieval, and search behavior which emulates a collection of objects".
     */
    val REPOSITORY = Tag("Repository")


    //org.springframework.boot.autoconfigure
    /**
     * Indicates a configuration class that declares one or more @Bean methods and also triggers auto-configuration and component scanning. This is a convenience annotation that is equivalent to declaring @Configuration, @EnableAutoConfiguration and @ComponentScan.
     */
    val SPRING_BOOT_APPLICATION = object : Tag("SpringBootApplication"){
        /**
         * valid params:
         * exclude : Class<?>[] -- Exclude specific auto-configuration classes such that they will never be applied..
         * required :  String[] -- Exclude specific auto-configuration class names such that they will never be applied.
         * scanBasePackageClasses :  Class<?>[] -- Type-safe alternative to scanBasePackages() for specifying the packages to scan for annotated components.
         * scanBasePackages :  String[] -- Base packages to scan for annotated components.
         */
        private val params = mapOf("exclude" to true,"excludeName" to false, "scanBasePackageClasses" to true, "scanBasePackages" to false)
        override fun validateParams(param: String): Boolean = params.containsKey(param)
        override fun withoutQuotation(param: String): Boolean = params.getValue(param)
    }

    /**
     * Indicates that a method produces a bean to be managed by the Spring container.
     */
    val BEAN = Tag("Bean")
}