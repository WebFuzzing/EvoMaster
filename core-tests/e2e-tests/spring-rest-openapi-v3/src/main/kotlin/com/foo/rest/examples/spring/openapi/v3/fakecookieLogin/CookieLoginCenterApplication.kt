package com.foo.rest.examples.spring.openapi.v3.fakecookieLogin

import com.foo.rest.examples.spring.openapi.v3.cookielogin.LoginDto
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/manager"])
open class CookieLoginCenterApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CookieLoginCenterApplication::class.java, *args)
        }
    }

    @PostMapping(path = ["/login"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login(@RequestBody login : LoginDto, response : HttpServletResponse) : ResponseEntity<String>{


        if(login.username == "bar" && login.password == "456"){
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
}