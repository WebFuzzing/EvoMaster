package com.foo.rest.examples.spring.openapi.v3.logintoken

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/logintoken"])
class LoginTokenRest {

    private val SECRET = "a complex secret..."

    @PostMapping(path = ["/login"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login(@RequestBody login : LoginDto) : ResponseEntity<AuthDto>{

        if(login.userId == "foo" && login.password == "123"){
            return ResponseEntity.ok(AuthDto("foo", TokenDto(SECRET)))
        }

        return ResponseEntity.status(400).build()
    }

    @GetMapping(path = ["/check"])
    open fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        if(authorization == "Bearer $SECRET"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(401).build()
    }
}