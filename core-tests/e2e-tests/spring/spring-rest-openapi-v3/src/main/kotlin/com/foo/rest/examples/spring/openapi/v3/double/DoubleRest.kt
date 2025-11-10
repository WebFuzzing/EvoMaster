package com.foo.rest.examples.spring.openapi.v3.double

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/double"])
class DoubleRest {


    @GetMapping(path = ["/{x}"])
    open fun get( @PathVariable("x") x: Double) : ResponseEntity<String> {

        if(x > 701.63 && x < 701.69){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}