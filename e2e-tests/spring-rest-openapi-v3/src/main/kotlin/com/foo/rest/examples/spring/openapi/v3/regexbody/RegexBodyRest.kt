package com.foo.rest.examples.spring.openapi.v3.regexbody

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
@RequestMapping(path = ["/api/regexbody"], consumes = [MediaType.APPLICATION_JSON_VALUE])
open class RegexBodyRest {

    @PostMapping
    open fun post(@Valid @RequestBody dto: RegexBodyDto) : ResponseEntity<String> {

        if(Pattern.matches("moo+",  dto.foo)){
            return ResponseEntity.ok("OK")
        }

        //this should be impossible to reach
        return ResponseEntity.status(500).build()
    }

}