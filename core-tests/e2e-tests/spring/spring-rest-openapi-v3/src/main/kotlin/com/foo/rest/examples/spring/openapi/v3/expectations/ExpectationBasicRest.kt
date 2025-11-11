package com.foo.rest.examples.spring.openapi.v3.expectations

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/basic/exp"])
class ExpectationBasicRest {

    @GetMapping(path = ["/okString/{s}"])
    fun getOkString(
            @PathVariable("s") succeeded: String
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Success! $succeeded")
    }

    @GetMapping(path = ["/badString/{s}"])
    fun getBadString(
            @PathVariable("s") succeeded: String
    ): ResponseEntity<String> {
        return ResponseEntity.status(500).build()
    }

    @GetMapping(path = ["/failString/{s}"])
    fun getFailString(
            @PathVariable("s") succeeded: String
    ): ResponseEntity<String> {
        throw IllegalArgumentException("I don't like negative numbers, and you gave me a $succeeded")
        //return ResponseEntity.ok("Success! $succeeded")
    }
}