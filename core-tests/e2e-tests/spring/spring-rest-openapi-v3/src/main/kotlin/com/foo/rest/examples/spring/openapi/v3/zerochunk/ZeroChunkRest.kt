package com.foo.rest.examples.spring.openapi.v3.zerochunk

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(path = ["/api/zerochunk"])
class ZeroChunkRest {

    // nothing written: with gzip on, the body is just an empty gzip stream
    @GetMapping("/empty")
    fun empty(response: HttpServletResponse) {
        response.status = 200
        response.contentType = "application/json"
        response.flushBuffer()
    }

    @GetMapping("/a")
    fun a(): String = "A"

    @GetMapping("/b")
    fun b(): String = "B"
}
