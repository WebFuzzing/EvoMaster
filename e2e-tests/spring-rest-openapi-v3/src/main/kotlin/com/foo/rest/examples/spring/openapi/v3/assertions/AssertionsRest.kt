package com.foo.rest.examples.spring.openapi.v3.assertions

import com.google.gson.Gson
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(path = ["/api/assertions"])
class AssertionsRest {

    @GetMapping(path = ["/data"])
    open fun getData() : ResponseEntity<String> {

        val assertionDto = Gson().toJson(AssertionDto())
        return ResponseEntity.status(200).body(assertionDto)
    }

    @PostMapping(path = ["/data"])
    open fun postData() : ResponseEntity<String> {
        return ResponseEntity.status(201)
                .body("OK")
    }

    @GetMapping(path = ["/simpleNumber"])
    open fun getSimpleNumber() : ResponseEntity<String> {
        return ResponseEntity.ok("42")
    }

    @GetMapping(path = ["/simpleString"])
    open fun getSimpleString() : ResponseEntity<String> {
        return ResponseEntity.ok("simple-string")
    }

    @GetMapping(path = ["/simpleText"])
    open fun getSimpleText() : ResponseEntity<String> {
        return ResponseEntity.status(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body("simple-text")
    }

    @GetMapping(path = ["/simpleArray"])
    open fun getSimpleArray() : ResponseEntity<String> {
        return ResponseEntity.status(200)
                .body(Gson().toJson(arrayOf(123, 456)))
    }

    @GetMapping(path = ["/arrayObject"])
    open fun getArrayObject() : ResponseEntity<String> {
        return ResponseEntity.status(200)
                .body(Gson().toJson(arrayOf(SimpleObject(777), SimpleObject(888))))
    }

    @GetMapping(path = ["/arrayEmpty"])
    open fun getArrayEmpty() : ResponseEntity<String> {
        return ResponseEntity.status(200)
                .body("[]")
    }

    @GetMapping(path = ["/objectEmpty"])
    open fun getObjectEmpty() : ResponseEntity<String> {
        return ResponseEntity.status(200)
                .body("{}")
    }

}