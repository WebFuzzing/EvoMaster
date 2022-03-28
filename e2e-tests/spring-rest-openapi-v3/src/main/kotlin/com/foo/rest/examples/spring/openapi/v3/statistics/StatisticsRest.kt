package com.foo.rest.examples.spring.openapi.v3.statistics

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/statistics"])
private class StatisticsRest {

    @GetMapping(path = ["/getExpectations/{b}"])
    open fun expectTest(@PathVariable("b") succeeded: Boolean ) : ResponseEntity<String> {
        if (succeeded){
            return ResponseEntity.ok("Success!")
        }
        else{
            return ResponseEntity.status(500).build()
        }

    }

    @GetMapping(path = ["/basicResponsesString/{b}"])
    open fun basicResponseString(@PathVariable("b") succeeded: Boolean) : ResponseEntity<String> {
        if (succeeded){
            return ResponseEntity.ok("Success!")
        }
        else{
            return ResponseEntity.status(400).build()
        }
    }

    @GetMapping(path = ["/basicNumbersString/{b}"])
    open fun basicResponsesNumber(@PathVariable("b") succeeded: Boolean) : ResponseEntity<String> {
        if (succeeded){
            return ResponseEntity.ok("42")
        }
        else{
            return ResponseEntity.ok("-1")
        }
    }
}
