package com.foo.rest.examples.spring.openapi.v3.expectations

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException

@RestController
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
        val result = if (succeeded >= 0) 42
                else return ResponseEntity.status(500).build()
                    //throw IllegalArgumentException("I don't like negative numbers, and you gave me a $succeeded")
        return ResponseEntity.ok(result)
    }

    // A test looking at getting the wrong input
    @GetMapping(path = ["/basicInput/{s}"])
    fun getInput(
            @PathVariable("s") succeeded: Boolean
    ): ResponseEntity<String> {
        val result = if (succeeded) 42
        else return ResponseEntity.status(500).build()
            //throw IllegalArgumentException("I don't like negative numbers, and you gave me a $succeeded")
        return ResponseEntity.ok("Response: ${result}")
    }

    // A test looking at wrong output type

    // A test looking at wrong output type
    @GetMapping(path = ["/responseObj/{s}"])
    fun getObject(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<OtherExampleObject> {
        val result = if (succeeded >= 0) {
            OtherExampleObject(succeeded, "object_$succeeded", "successes")
        } else {
            OtherExampleObject()
        }

        return ResponseEntity.ok(result)
    }

    // A test looking at wrong output structure
    @GetMapping(path = ["/responseUnsupObj/{s}"])
    fun getUnsupObject(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<ExampleObject> {
        val result = if (succeeded >= 0) {
            ExampleObject(succeeded, "validObject_$succeeded", "successful")
        } else {
            ExampleObject(succeeded, "object_$succeeded", "failed")
        }
        return ResponseEntity.ok(result)
    }

    // A test looking at an array of returned objects

    // A test looking at an array of returned objects
    @GetMapping(path = ["/responseMultipleObjs/{s}"])
    fun getMultipleObjects(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<Array<GenericObject>> {
        val result = if (succeeded >= 0) {
            arrayOf<GenericObject>(
                    ExampleObject(succeeded, "validObject_$succeeded", "successful"),
                    ExampleObject(succeeded + 1, "validObject_" + (succeeded + 1), "successful")
            )
        } else {
            arrayOf<GenericObject>(
                    OtherExampleObject(),
                    OtherExampleObject(succeeded, "object_$succeeded", "successes")
            )
        }
        return ResponseEntity.ok(result)
    }

}