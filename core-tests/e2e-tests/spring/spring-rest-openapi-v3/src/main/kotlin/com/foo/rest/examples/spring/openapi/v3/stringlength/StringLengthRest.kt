package com.foo.rest.examples.spring.openapi.v3.stringlength

import com.foo.rest.examples.spring.openapi.v3.charescaperegex.CharEscapeRegexDto
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.regex.Pattern
import javax.validation.Valid

@RestController
@RequestMapping(path = ["/api/stringlength"], consumes = [MediaType.APPLICATION_JSON_VALUE])
open class StringLengthRest {

    @PostMapping(path = ["/long"])
    open fun long(@Valid @RequestBody dto: StringLengthDto) : ResponseEntity<String> {

        if(dto.foo!!.length > 10){
            return ResponseEntity.ok("OK")
        }

        //this should be impossible to reach
        return ResponseEntity.status(500).build()
    }


    @PostMapping(path = ["/taint"])
    open fun taint(@Valid @RequestBody dto: StringLengthDto) : ResponseEntity<String> {

        if(dto.foo == "A very loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
            "ooooooooooooooooooooooooong string with way too many characters, which should be nearly impossible to" +
            " get without taint analysis"){
            return ResponseEntity.ok("OK")
        }

        //this should be impossible to reach
        return ResponseEntity.status(500).build()
    }


}