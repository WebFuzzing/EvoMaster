package com.foo.mcp.bb.examples.spring.holidays

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class HolidaysApplication {

    fun main(args: Array<String>) {
        SpringApplication.run(HolidaysApplication::class.java, *args)
    }

}
