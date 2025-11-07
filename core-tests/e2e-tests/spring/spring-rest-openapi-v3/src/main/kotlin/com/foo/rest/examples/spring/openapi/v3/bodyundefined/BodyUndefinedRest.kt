package com.foo.rest.examples.spring.openapi.v3.bodyundefined

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(path = ["/api/bodyundefined"])
class BodyUndefinedRest {

    @RequestMapping(method = [RequestMethod.GET,RequestMethod.DELETE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun send(@RequestBody(required = true) body : BodyUndefinedDto) : ResponseEntity<String>{

        if(body.user != null){
            return ResponseEntity.status(200).build()
        }

        return ResponseEntity.status(400).build()
    }


}