package com.foo.rest.examples.spring.openapi.v3.flakinessdetect

import org.h2.util.MathUtils.randomInt
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

@RestController
@RequestMapping(path = ["/api/flakinessdetect"])
class FlakinessDetectRest {

    companion object{
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd EEEE 'Week' ww")
    }

    @GetMapping(path = ["/stringfirst/{n}"])
    open fun getFirst( @PathVariable("n") n: Int) : ResponseEntity<String> {

        return ResponseEntity.ok(getPartialDate(n))
    }

    @GetMapping(path = ["/next/{n}"])
    open fun getNext( @PathVariable("n") n: Int) : ResponseEntity<FlakinessDetectData> {

        return ResponseEntity.ok(FlakinessDetectData(getPartialDate(n), randomInt(n)))
    }


    private fun getPartialDate(n: Int) : String {
        val now = LocalDateTime.now().format(formatter)
        val size = max(12, min(now.toString().length, n))

        return now.substring(0, size)
    }
}

data class FlakinessDetectData(val first : String, val next : Int)