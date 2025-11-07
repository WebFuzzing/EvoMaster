package com.foo.rest.examples.spring.openapi.v3.timeout

import com.google.gson.Gson
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/timeout"])
class TimeoutRest {

    @GetMapping
    open fun getData() : ResponseEntity<String> {

        Thread.sleep(10_000)

        return ResponseEntity.status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("HELLO!!!")
    }
}