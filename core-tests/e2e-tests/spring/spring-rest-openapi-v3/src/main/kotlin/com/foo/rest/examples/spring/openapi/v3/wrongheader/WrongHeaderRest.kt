package com.foo.rest.examples.spring.openapi.v3.wrongheader

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/wrongheader"])
open class WrongHeaderRest {

    @GetMapping
    open fun check() : ResponseEntity<String> {

        return ResponseEntity.status(401)
            //empty value on purpose
            .header("WWW-Authenticate", "")
            .body("FAIL")
    }
}