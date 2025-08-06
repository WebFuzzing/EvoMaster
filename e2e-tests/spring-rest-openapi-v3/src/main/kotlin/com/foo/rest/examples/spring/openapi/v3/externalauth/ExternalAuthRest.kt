package com.foo.rest.examples.spring.openapi.v3.externalauth

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(path = ["/api/externalauth"])
class ExternalAuthRest {

    @PostMapping(path = ["/login1"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login(@RequestBody login : LoginDto, response : HttpServletResponse) : ResponseEntity<String>{
        if(login.username == "foo" && login.password == "123"){
            return ResponseEntity.ok("{\"access_token\":\"token1\"}")
        }
        return ResponseEntity.status(401).build()
    }

    @PostMapping(path = ["/login2"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login2(@RequestBody login : LoginDto, response : HttpServletResponse) : ResponseEntity<String>{
        if(login.username == "foo" && login.password == "123"){
            return ResponseEntity.ok("{\"access_token\":\"token2\"}")
        }
        return ResponseEntity.status(401).build()
    }

    @GetMapping(path = ["/check"])
    open fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        if(authorization.isNullOrEmpty()){
            return ResponseEntity.status(401).build()
        }
        if(authorization == "token1" || authorization == "token2")
            return ResponseEntity.ok(authorization)

        return ResponseEntity.status(401).build()
    }
}