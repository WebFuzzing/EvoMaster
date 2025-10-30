package com.foo.rest.examples.spring.openapi.v3.dto

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DtoRest {

    @PostMapping(path = ["/object"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun obj(@RequestBody body: PersonDto) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/array"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun arr(@RequestBody body: List<PersonDto>) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/array-of-string"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun arrOfString(@RequestBody body: List<String>) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/string"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun str(@RequestBody body: String) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/number"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun num(@RequestBody body: Double) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/integer"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun ints(@RequestBody body: Integer) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/integer-no-format"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun intNoFormat(@RequestBody body: Integer) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/boolean"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun bool(@RequestBody body: Boolean) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

}
