package com.foo.rest.examples.spring.openapi.v3.extraheader

import com.sun.net.httpserver.HttpExchange
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLDecoder
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(path = ["/api/extraheader"])
class ExtraHeaderRest {

    @GetMapping()
    open fun getHeader(@RequestHeader(name="x-a", required = true) a : String): String {

        return if (a == null) {
            "FALSE"
        } else "OK"
    }


}