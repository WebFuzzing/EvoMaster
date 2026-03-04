package com.foo.mcp.bb.examples.spring.holidays

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication
open class HolidaysApplication {

//    companion object {
//
//    }

//    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication.run(HolidaysApplication::class.java, *args)
    }

}
