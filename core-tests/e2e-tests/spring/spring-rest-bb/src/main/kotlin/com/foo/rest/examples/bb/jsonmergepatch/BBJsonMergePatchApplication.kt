package com.foo.rest.examples.bb.jsonmergepatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.Optional

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/api/jsonmergepatch")
open class BBJsonMergePatchApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBJsonMergePatchApplication::class.java, *args)
        }
    }



    @PatchMapping("/", consumes = ["application/merge-patch+json"])
    fun patchPet(@RequestBody dto: BBJsonMergePatchDto): ResponseEntity<String> {

        if(dto.name == null && dto.x == null){
            CoveredTargets.cover("BOTH_UNDEFINED")
            return ResponseEntity.status(400).build()
        }
        if(dto.name == null || dto.x == null){
            CoveredTargets.cover("ONE_UNDEFINED")
            return ResponseEntity.status(401).build()
        }
        //we use different status codes just to make sure tests end up in final test suite
        if(dto.name.isEmpty && dto.x.isEmpty){
            CoveredTargets.cover("BOTH_NULL")
            return ResponseEntity.status(403).build()
        }
        if(!dto.name.isEmpty && !dto.x.isEmpty){
            CoveredTargets.cover("BOTH_PRESENT")
            return ResponseEntity.status(200).build()
        }
        if(dto.name.isEmpty || dto.x.isEmpty){
            CoveredTargets.cover("ONE_PRESENT")
            return ResponseEntity.status(404).build()
        }

        //should never be reached
        return ResponseEntity.ok("patched")
    }
}

data class BBJsonMergePatchDto(
    @field:Schema(nullable = true)
    val name: Optional<String>? = null,

    @field:Schema(nullable = true)
    val x: Optional<Int>? = null
)