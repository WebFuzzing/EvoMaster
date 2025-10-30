package com.foo.rest.examples.spring.openapi.v3.aiclassification.arithmetic

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/arithmetic"])
class ACArithmeticRest {
    @GetMapping
    open fun get(
        @RequestParam("x") x: Int?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Double?,
        @RequestParam("k" )k: Double?,
        @RequestParam("s") s: String?,
    ) : ResponseEntity<String> {

        if(! (x!! < y!!)){
            return ResponseEntity.status(400).build()
        }

        if(z != null && k != null) {
            if (!(z >= k)) {
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACArithmeticDto) : ResponseEntity<String> {

        val x = body.c!!
        val y = body.e!!
        val z = body.f!!
        val k = body.g!!

        if(x == y || z < k ){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }


}