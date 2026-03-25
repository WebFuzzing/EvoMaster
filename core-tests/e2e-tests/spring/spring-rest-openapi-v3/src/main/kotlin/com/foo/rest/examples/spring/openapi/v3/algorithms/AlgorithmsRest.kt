package com.foo.rest.examples.spring.openapi.v3.algorithms

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/algorithms"])
class AlgorithmsRest {

    @GetMapping(path = ["/int/{x}"])
    open fun getInt( @PathVariable("x") x: Int) : ResponseEntity<String> {

        if(x >= 0){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }

    @GetMapping(path = ["/double/{x}"])
    open fun getDouble( @PathVariable("x") x: Double) : ResponseEntity<String> {

        if(x >= 0){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }

}