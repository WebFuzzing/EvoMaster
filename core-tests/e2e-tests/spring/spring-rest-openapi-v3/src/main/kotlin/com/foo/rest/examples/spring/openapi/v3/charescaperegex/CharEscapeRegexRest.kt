package com.foo.rest.examples.spring.openapi.v3.charescaperegex

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.regex.Pattern

@RestController
@RequestMapping(path = ["/api/charescaperegex"], consumes = [MediaType.APPLICATION_JSON_VALUE])
open class CharEscapeRegexRest {

    @PostMapping(path = ["/x"])
    open fun getX( @RequestBody dto: CharEscapeRegexDto) : ResponseEntity<String> {

        if(Pattern.matches("\\s\\S\\d\\D\\w\\W",  dto.value)){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }


    @PostMapping(path = ["/y"])
    open fun getY( @RequestBody dto: CharEscapeRegexDto) : ResponseEntity<String> {

        if(Pattern.matches("\\s", dto.value)){
            if(dto.value == "\n")
                return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }


    @PostMapping(path = ["/z"])
    open fun getZ( @RequestBody dto: CharEscapeRegexDto) : ResponseEntity<String> {

        if(Pattern.matches("\\d",  dto.value)){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }


}