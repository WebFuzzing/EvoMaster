package com.foo.rest.examples.spring.openapi.v3.enum

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/enum"])
class EnumRest {


    @GetMapping
    open fun get(@RequestParam x: String?, @RequestParam y: Int?) : ResponseEntity<String> {

        if(x == "foo" && y == 1234567){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun post(@RequestBody dto: EnumDto) : ResponseEntity<String>{

        if(dto.values[0] == "foo"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}