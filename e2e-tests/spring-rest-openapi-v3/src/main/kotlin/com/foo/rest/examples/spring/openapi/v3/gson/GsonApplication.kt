package com.foo.rest.examples.spring.openapi.v3.gson

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class GsonApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(GsonApplication::class.java, *args)
        }
    }


}

