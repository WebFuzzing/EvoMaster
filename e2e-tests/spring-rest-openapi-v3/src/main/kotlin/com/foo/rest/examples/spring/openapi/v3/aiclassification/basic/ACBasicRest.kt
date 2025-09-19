package com.foo.rest.examples.spring.openapi.v3.aiclassification.basic

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/basic"])
class ACBasicRest {

    @GetMapping
    open fun getData(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        //no dependency, just constraint on a single variable

        if(y == null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

}