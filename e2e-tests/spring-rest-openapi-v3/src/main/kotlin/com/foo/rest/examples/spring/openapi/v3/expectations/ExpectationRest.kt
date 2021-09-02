package com.foo.rest.examples.spring.openapi.v3.expectations

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException

@Controller
@RequestMapping(path = ["/api/expectations"])
class ExpectationRest {

    @GetMapping(path = ["/basicResponsesString/{s}"])
    fun getString(
            @PathVariable("s") succeeded: String
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Success! $succeeded")
    }

    @GetMapping(path = ["/basicResponsesNumeric/{s}"])
    fun getNumeric(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<Int> {
        if (succeeded < 0) throw IllegalArgumentException("I don't like negative numbers, and you gave me a $succeeded")
        return ResponseEntity.ok(42)
    }

    // A test looking at getting the wrong input
    @GetMapping(path = ["/basicInput/{s}"])
    fun getInput(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<String> {
        if (succeeded >= 0) throw IllegalArgumentException("I don't like negative numbers, and you gave me a $succeeded")
        val result = 42
        return ResponseEntity.ok("Response: $result")
    }

    // A test looking at wrong output type
    @GetMapping(path = ["/responseObj/{s}"])
    fun getObject(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<OtherExampleObject> {
        val result = OtherExampleObject(succeeded, "object_$succeeded", "successes")
        return ResponseEntity.ok(result)
    }

    // A test looking at wrong output structure
    @GetMapping(path = ["/responseUnsupObj/{s}"])
    fun getUnsupObject(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<ExampleObject> {
        val result = ExampleObject(succeeded, "validObject_$succeeded", "successful")
        return ResponseEntity.ok(result)
    }

    // A test looking at an array of returned objects
    @GetMapping(path = ["/responseMultipleObjs/{s}"])
    fun getMultipleObjects(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<Array<GenericObject>> {
        val result = arrayOf<GenericObject>(
                    OtherExampleObject(succeeded, "object_$succeeded", "successes"),
                    ExampleObject(succeeded + 1, "validObject_${succeeded + 1}", "successful")
            )
        return ResponseEntity.ok(result)
    }

}