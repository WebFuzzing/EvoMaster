package com.foo.rest.examples.bb.datapool

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbdatapool"])
@RestController
open class BBDataPoolApplication {

    private val x = BBDataPoolDto("userX123456", "foo", "bar")
    private val y = BBDataPoolDto("userY777777", "hello", "there")
    private val z = BBDataPoolDto("userZ984750", "john", "smith")

    private val data = listOf(x,y,z)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBDataPoolApplication::class.java, *args)
        }
    }

    @GetMapping("/users")
    open fun getUsers() : ResponseEntity<BBDataPoolDtoWrapper> {

        val payload = BBDataPoolDtoWrapper(data = data)

        return ResponseEntity.status(200).body(payload)
    }


    @GetMapping("/users/{id}")
    open fun getUser(@PathVariable("id") id: String) : ResponseEntity<BBDataPoolDto> {

        val payload = data.find { it.id == id }
            ?: return ResponseEntity.status(404).build()

        CoveredTargets.cover("OK")
        return ResponseEntity.status(200).body(payload)
    }


    @DeleteMapping("/users/{id}")
    open fun deleteUser(@PathVariable("id") id: String) : ResponseEntity<BBDataPoolDto> {

        val payload = data.find { it.id == id }
            ?: return ResponseEntity.status(404).build()

        return ResponseEntity.status(200).body(payload)
    }

    @PutMapping("/users/{id}")
    open fun replaceUser(@PathVariable("id") id: String, @RequestBody body: BBDataPoolDto) : ResponseEntity<BBDataPoolDto> {

        val payload = data.find { it.id == id }
            ?: return ResponseEntity.status(404).build()

        return ResponseEntity.status(200).body(payload)
    }

    @PatchMapping("/users/{id}")
    open fun modifyUser(@PathVariable("id") id: String, @RequestBody body: BBDataPoolDto) : ResponseEntity<BBDataPoolDto> {

        val payload = data.find { it.id == id }
            ?: return ResponseEntity.status(404).build()

        return ResponseEntity.status(200).body(payload)
    }


}