package com.foo.rest.examples.spring.openapi.v3.aiclassification.or

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/or"])
class ACOrRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        // x or z=true
        if(! (x!=null || z == true )){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACOrDto) : ResponseEntity<String> {

        // a=true or b
        if(! (body.a == true || body.b != null )){
            return ResponseEntity.status(400).build()
        }

        // e or f=false
        if(! (body.f == false || body.e != null )){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }

}