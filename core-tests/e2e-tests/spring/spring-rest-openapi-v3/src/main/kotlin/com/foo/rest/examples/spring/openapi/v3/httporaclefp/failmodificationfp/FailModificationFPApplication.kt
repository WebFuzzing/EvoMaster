package com.foo.rest.examples.spring.openapi.v3.httporaclefp.failmodificationfp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class FailModificationFPApplication {


    @PutMapping
    open fun put(
        @RequestBody body: FailModificationFPDto,
    ): ResponseEntity<Any> {

        return ResponseEntity.status(400).build()
    }


    @GetMapping
    open fun get(): ResponseEntity<FailModificationFPDto> {

        return ResponseEntity.status(200).body(
            FailModificationFPDto(timestamp = System.currentTimeMillis(), message = null)
        )
    }
}