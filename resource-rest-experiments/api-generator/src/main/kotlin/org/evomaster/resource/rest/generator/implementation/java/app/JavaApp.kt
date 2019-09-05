package org.evomaster.resource.rest.generator.implementation.java.app

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SwaggerAnnotation
import org.evomaster.resource.rest.generator.model.AppClazz
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-08-20
 */
class JavaApp (specification: AppClazz) : JavaClass<AppClazz>(specification) {
    override fun getImports(): List<String> = listOf(
            "org.springframework.context.annotation.Bean",
            "org.springframework.security.core.Authentication",
            "org.springframework.web.context.request.WebRequest",
            "org.springframework.boot.SpringApplication",
            "org.springframework.boot.autoconfigure.SpringBootApplication",
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
            "springfox.documentation.swagger2.annotations.EnableSwagger2",
            "springfox.documentation.builders.ApiInfoBuilder",
            "springfox.documentation.service.ApiInfo",
            "springfox.documentation.spi.DocumentationType",
            "springfox.documentation.spring.web.plugins.Docket",
            "static springfox.documentation.builders.PathSelectors.regex"
    )

    override fun getMethods(): List<MethodScript> = listOf(
            JavaDockApiMethod(),
            ApiInfoMethod(),
            MainMethod(specification)
    )

    override fun generateConstructors(types: RegisterType): List<String> = listOf()

    override fun getDeclaration(): List<DeclarationScript>  = listOf()

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String> = listOf()

    override fun getTags(): List<String> = listOf(
            SwaggerAnnotation.ENABLE_SWAGGER_2.getText(),
            SpringAnnotation.SPRING_BOOT_APPLICATION.getText(mapOf("exclude" to "SecurityAutoConfiguration.class"))
    ).map { "@$it" }

    override fun getName(): String = specification.name
}