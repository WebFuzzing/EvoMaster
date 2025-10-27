package com.foo.rest.examples.spring.openapi.v3.taintcase

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/taintcase"])
class TaintCaseRest {

    @GetMapping(path = ["/check/{a}/{b}"])
    open fun check(@PathVariable("a") a: String, @PathVariable("b") b: String) : ResponseEntity<String> {
        return if (a.lowercase().equals("hello") && b.uppercase().equals("THERE")){
//            return if (a.equals("hello") && b.equals("THERE")){
            ResponseEntity.ok("OK")
        }
        else{
            ResponseEntity.status(500).build()
        }

    }
}