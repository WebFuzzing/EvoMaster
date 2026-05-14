package com.foo.rest.examples.spring.openapi.v3.Http429Long

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@RestController
@RequestMapping(path = ["/api/http429long"])
class Http429LongRest {


    @GetMapping
    fun get() : ResponseEntity<String> {

        val tomorrow = DateTimeFormatter.RFC_1123_DATE_TIME.format(
            Instant.now().plus(24, ChronoUnit.HOURS)
                .atZone(ZoneOffset.UTC)
        )

        return ResponseEntity.status(429).header("Retry-After",tomorrow).build()
    }

}
