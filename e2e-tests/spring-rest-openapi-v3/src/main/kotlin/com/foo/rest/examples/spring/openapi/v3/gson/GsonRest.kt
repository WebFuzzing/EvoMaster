package com.foo.rest.examples.spring.openapi.v3.gson

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(path = ["/api/gson"])
class GsonRest {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun post(request: HttpServletRequest) : ResponseEntity<String> {

        val json = IOUtils.toString(request.inputStream)

        val dto = Gson().fromJson(json, FooDto::class.java)

        if (dto.x > 0) return ResponseEntity.ok("Hello World!!!")
        else throw IllegalArgumentException("Failed Call")
    }
}


class FooDto(var x : Int)