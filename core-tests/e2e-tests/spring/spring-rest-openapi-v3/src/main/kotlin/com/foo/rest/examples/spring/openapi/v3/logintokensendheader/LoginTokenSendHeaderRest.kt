package com.foo.rest.examples.spring.openapi.v3.logintokensendheader

import com.foo.rest.examples.spring.openapi.v3.logintoken.AuthDto
import com.foo.rest.examples.spring.openapi.v3.logintoken.LoginDto
import com.foo.rest.examples.spring.openapi.v3.logintoken.TokenDto
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/logintokensendheader"])
class LoginTokenSendHeaderRest {

    private val SECRET = "a complex secret..."

    @PostMapping(path = ["/login"])
    fun login(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<*>{

        if(authorization == "foo 123"){
            return ResponseEntity.ok().header("X-Auth-Token", SECRET).build<Any>()
        }

        return ResponseEntity.status(400).build<Any>()
    }

    @GetMapping(path = ["/check"])
    fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        if(authorization == "Bearer $SECRET"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(401).build()
    }
}