package com.foo.rest.examples.spring.openapi.v3.http429short

import com.foo.rest.examples.spring.openapi.v3.enum.EnumDto
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/http429short"])
class Http429ShortRest {

    private var tooMany = false

    @GetMapping
    fun get() : ResponseEntity<String> {

        tooMany = !tooMany

        if(tooMany) {
            return ResponseEntity.status(429).header("Retry-After","1").build()
        }

        return ResponseEntity.ok("OK")
    }

}
