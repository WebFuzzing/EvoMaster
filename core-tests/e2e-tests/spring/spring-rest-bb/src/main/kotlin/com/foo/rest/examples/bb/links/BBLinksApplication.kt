package com.foo.rest.examples.bb.links

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping(path = ["/api/links"])
open class BBLinksApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBLinksApplication::class.java, *args)
        }
    }

    @Volatile
    private var code = 0

    @PostMapping(path = ["/create"])
    fun postCreate() : ResponseEntity<BBLinksDto> {

        code++ //make sure this is always dynamic

        return ResponseEntity.status(200).body(BBLinksDto(
            data = BBLinksDataDto("FOO", code)
        ))
    }

    @GetMapping(path = ["/users/{name}/{code}"])
    fun getUser(
        @PathVariable("name") pathName: String,
        @PathVariable("code") pathCode: Int,
        @RequestParam("name") queryName: String
    ) : ResponseEntity<String>{

        if(pathName != "FOO" || pathCode != code || queryName != "BAR"){
            CoveredTargets.cover("WRONG")
            return ResponseEntity.status(400).body("WRONG")
        }

        CoveredTargets.cover("OK")
        return ResponseEntity.status(200).body("OK")
    }
}