package com.foo.rest.examples.bb.authcookie

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class CookieLoginApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CookieLoginApplication::class.java, *args)
        }
    }
}