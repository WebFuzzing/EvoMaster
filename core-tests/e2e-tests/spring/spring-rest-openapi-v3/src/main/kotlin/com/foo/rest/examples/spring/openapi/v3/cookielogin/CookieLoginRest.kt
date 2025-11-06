package com.foo.rest.examples.spring.openapi.v3.cookielogin

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(path = ["/api/logintoken"])
class CookieLoginRest {

    @PostMapping(path = ["/login"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login(@RequestBody login : LoginDto, response : HttpServletResponse) : ResponseEntity<String>{


        if(login.username == "foo" && login.password == "123"){
            // create a cookie
            val cookie = Cookie("username", login.username)
            cookie.maxAge = 7 * 24 * 60 * 60 // expires in 7 days

            cookie.secure = true
            cookie.isHttpOnly = true

            response.addCookie(cookie)
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }

    @GetMapping(path = ["/check"])
    open fun check(@CookieValue("username") authorization: String?) : ResponseEntity<String>{

        if(authorization.isNullOrEmpty()){
            return ResponseEntity.status(401).build()
        }

        val token = if (authorization == "foo") "this:" else if (authorization == "bar") "center:" else ""

        return ResponseEntity.ok(token + authorization)
    }
}