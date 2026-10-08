package com.foo.rest.examples.spring.openapi.v3.aiclassification.defaultexample

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RequestMapping(path = ["/api/defaultexample"])
@RestController
open class ACDefaultExampleRest {


    @GetMapping
    open fun getData(
        @RequestParam("x") x: Int?, //examples: 11223344
        @RequestParam("y") y: Int?, //default 42
        @RequestParam("z") z: Int?,
    ): ResponseEntity<String> {


        if(x!=null && y!=null && y < x){
            //if 'x' using example, then hard to pass this constraint
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }
}