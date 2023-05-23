package com.foo.rest.examples.spring.openapi.v3.cluster

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/cluster"])
@RestController
open class ClusterApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ClusterApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/path1/{success}"])
    open fun get(@PathVariable("success") success: Boolean) : ResponseEntity<String> {
        return externalMethodA(success)
    }

    @GetMapping(path = ["/path2/{success}"])
    open fun timeout(@PathVariable("success") success: Boolean) : ResponseEntity<String> {
        return externalMethodZ(success)
    }

    @GetMapping(path = ["/path3/{success}"])
    open fun option3(@PathVariable("success") success: Boolean) : ResponseEntity<String>{
        return externalMethodZ(success)
    }

    @GetMapping(path = ["/path4/{success}"])
    open fun option4(@PathVariable("success") success: Boolean) : ResponseEntity<String>{
        return externalMethodA(success)
    }


    fun externalMethodA(success: Boolean) : ResponseEntity<String>{
        if (success) return ResponseEntity.ok("Hello World!!!")
        else throw IllegalArgumentException("aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa")
        //else return ResponseEntity.status(500).body("aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa aaaaa")

    }

    fun externalMethodZ(success: Boolean) : ResponseEntity<String>{
        if (success) return ResponseEntity.ok("Hullo Again!")
        else throw IllegalArgumentException("zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz")
        //else return ResponseEntity.status(500).body("zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz zzzzz")
    }
}