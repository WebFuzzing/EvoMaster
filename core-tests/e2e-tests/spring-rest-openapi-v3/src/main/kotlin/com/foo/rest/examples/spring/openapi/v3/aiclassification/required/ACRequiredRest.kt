package com.foo.rest.examples.spring.openapi.v3.aiclassification.required

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/required"])
class ACRequiredRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        if(x==null || y == null || z == null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACRequiredDto) : ResponseEntity<String> {

        if(body.b == null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }


    @PutMapping
    open fun put(@RequestBody(required = true) body : ACRequiredDto,
                 @RequestParam("x") x: String?
    ) : ResponseEntity<String> {

        if(body.a == null || body.b == null || body.c == null || x == null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(204).body("OK")
    }


}