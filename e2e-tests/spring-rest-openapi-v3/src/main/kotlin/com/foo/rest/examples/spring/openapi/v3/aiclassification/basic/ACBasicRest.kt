package com.foo.rest.examples.spring.openapi.v3.aiclassification.basic

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.ws.rs.QueryParam

@RestController
@RequestMapping(path = ["/api/basic"])
class ACBasicRest {

    @GetMapping
    open fun getData(
        @QueryParam("x") x: String?,
        @QueryParam("y") y: Int?,
        @QueryParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        //no dependency, just constraint on single variable

        if(y == null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

}