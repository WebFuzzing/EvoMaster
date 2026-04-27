package com.foo.rest.examples.spring.openapi.v3.statusoracle

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/statusoracle"])
private class StatisticsRest {

    @GetMapping(path = ["/no-non-standard-codes/42"])
    fun noNonStandardCodes42() : ResponseEntity<String> {
        return ResponseEntity.status(42).build()
    }

    @GetMapping(path = ["/no-non-standard-codes/912"])
    fun noNonStandardCodes912() : ResponseEntity<String> {
        return ResponseEntity.status(912).build()
    }

    @GetMapping(path = ["/no-non-standard-codes/1024"])
    fun noNonStandardCodes1024() : ResponseEntity<String> {
        return ResponseEntity.status(1024).build()
    }

}
