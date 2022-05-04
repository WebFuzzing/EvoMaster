package com.foo.rest.examples.spring.openapi.v3.boottimetargets

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping(path = ["/api/boottimetargets"])
class BootTimeTargetsRest {

    companion object{

        private val STARTUP = "${LocalDateTime.now()}"

        init {
            println(STARTUP)
        }

    }


    @GetMapping
    open fun getData() : ResponseEntity<String> {

        return ResponseEntity.status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("$STARTUP;${LocalDateTime.now()}")
    }
}