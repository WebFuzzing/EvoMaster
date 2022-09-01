package com.foo.rest.examples.spring.openapi.v3.uri

import com.foo.rest.examples.spring.openapi.v3.regexbody.RegexBodyDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import javax.validation.Valid

@RestController
@RequestMapping(path = ["/api/uri"])
class UriRest {

    @PostMapping(path = ["http"])
    open fun http(@Valid @RequestBody dto: UriDto) : ResponseEntity<String> {

        val url = URL(dto.x);
        if(url.protocol == "http"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(500).build()
    }

    @PostMapping(path = ["data"])
    open fun data(@Valid @RequestBody dto: UriDto) : ResponseEntity<String> {


        val uri = URI(dto.x)
        if(uri.scheme == "data"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(500).build()
    }

}