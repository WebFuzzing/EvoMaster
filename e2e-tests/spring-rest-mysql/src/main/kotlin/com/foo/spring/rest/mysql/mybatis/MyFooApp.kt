package com.foo.spring.rest.mysql.mybatis

import com.foo.spring.rest.mysql.SwaggerConfiguration
import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import springfox.documentation.swagger2.annotations.EnableSwagger2

@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class MyFooApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MyFooApp::class.java, *args)
        }
    }
}