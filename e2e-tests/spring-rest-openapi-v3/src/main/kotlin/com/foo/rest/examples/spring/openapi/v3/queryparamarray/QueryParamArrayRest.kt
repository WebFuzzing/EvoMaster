package com.foo.rest.examples.spring.openapi.v3.queryparamarray

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RequestMapping(path = ["/api/queryparamarray"])
@RestController
class QueryParamArrayRest {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun getInt(
        @RequestParam("x", required = true) x : List<Int>
    ) : ResponseEntity<String> {

        if(x.isEmpty()){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok( "[" +  x.joinToString(",") + "]")
    }
}