package com.foo.rest.examples.spring.openapi.v3.boottimetargets

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping(path = ["/api/boottimetargets"])
class BootTimeTargetsRest {

    private val startupTime  = LocalDateTime.now()
    private var startupInfo : String

    init {
        startupInfo = "${branchTarget()}:$startupTime"
        println("startupInfo:$startupInfo")
    }

    companion object{
        // TODO targets for lines 24 and 25 seems to be skipped, need a further check once class initializer is instrumented
        private const val SKIPPED_LINE = "SKIPPED?"
        init {
            println("Hello $SKIPPED_LINE")
        }
    }


    @GetMapping
    open fun getData() : ResponseEntity<String> {

        return ResponseEntity.status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("$startupTime;${LocalDateTime.now()}")
    }

    private fun branchTarget() : String{
        return if (startupTime.second % 2 == 0)
            "EVEN"
        else
            "ODD"
    }

}