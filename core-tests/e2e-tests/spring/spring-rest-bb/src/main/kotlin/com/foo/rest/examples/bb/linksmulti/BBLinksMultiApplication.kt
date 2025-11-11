package com.foo.rest.examples.bb.linksmulti

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping(path = ["/api/linksmulti"])
open class BBLinksMultiApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBLinksMultiApplication::class.java, *args)
        }
    }

    private val secretCode = 13214254

    @PostMapping(path = ["/x"])
    fun x(@RequestParam("code") code: Int?) : ResponseEntity<BBLinksMultiDto> {

        if(code == secretCode){
            CoveredTargets.cover("CODE")
            return ResponseEntity.status(201).body(BBLinksMultiDto("HELLO"))
        }

        return ResponseEntity.status(200).body(BBLinksMultiDto("FOO", "BAR"))
    }

    @PostMapping(path = ["/y"])
    fun y(
        @RequestParam("first") first: String?,
        @RequestParam("second") second: String?,
    ) : ResponseEntity<BBLinksMultiDto> {

        if(first == "FOO" && second == "BAR"){
            CoveredTargets.cover("FOOBAR")
            return ResponseEntity.status(200).body(BBLinksMultiDto(c = secretCode))
        }

        return ResponseEntity.status(400).build()
    }


    @PostMapping(path = ["/z"])
    fun z(@RequestParam("data") data: String?) : ResponseEntity<BBLinksMultiDto> {

        if(data == "HELLO"){
            CoveredTargets.cover("HELLO")
            return ResponseEntity.status(200).build()
        }

        return ResponseEntity.status(400).build()
    }

}