package com.foo.rest.examples.spring.openapi.v3.overlay

import com.foo.rest.examples.spring.openapi.v3.charescaperegex.CharEscapeRegexDto
import com.foo.rest.examples.spring.openapi.v3.stringlength.StringLengthDto
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.regex.Pattern
import javax.validation.Valid
import javax.ws.rs.QueryParam

@RestController
@RequestMapping(path = ["/api/overlay"])
open class OverlayRest {

    @GetMapping
    open fun get( @QueryParam("x") x: String, @QueryParam("y") y: String) : ResponseEntity<String>{

        return ResponseEntity.ok("x=$x , y=$y")
    }

}