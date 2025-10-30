package com.foo.rest.examples.spring.openapi.v3.uuid

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/uuid"])
class UuidRest {

    @GetMapping(path = ["/{a}"])
    open fun check(@PathVariable("a") a: String) : ResponseEntity<String> {

        val x = UUID.fromString(a)

        return if (x != null){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }
    }
}