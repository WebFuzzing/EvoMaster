package com.foo.rest.examples.bb.authcreateusers

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class BBAuthCreateUsersApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBAuthCreateUsersApplication::class.java, *args)
        }
    }
}