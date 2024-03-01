package com.foo.rest.examples.spring.openapi.v3.minimize

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/minimize"])
class MinimizeRest {


    @GetMapping(path = ["/{x}"])
    open fun get( @PathVariable("x") x: Int) : ResponseEntity<String> {

        if(x < 0) return ResponseEntity.ok("a")
        if(x < 100) return ResponseEntity.ok("b")
        if(x < 1_000) return ResponseEntity.ok("c")
        if(x < 10_000) return ResponseEntity.ok("d")

        return return ResponseEntity.ok("e")
    }
}