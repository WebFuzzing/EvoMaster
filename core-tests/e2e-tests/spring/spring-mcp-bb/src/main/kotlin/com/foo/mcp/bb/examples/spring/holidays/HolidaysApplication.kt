package com.foo.mcp.bb.examples.spring.holidays

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class HolidaysApplication {

    @Bean
    open fun holidayTools(holidayService: HolidayService): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(holidayService)
            .build()
    }

}

fun main(args: Array<String>) {
    SpringApplication.run(HolidaysApplication::class.java, *args)
}
