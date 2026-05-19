package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.json

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/accounts"])
@RestController
open class HttpNonIdempotentPutApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpNonIdempotentPutApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, AccountData>()

        fun reset(){
            data.clear()
        }
    }

    data class AccountData(
        var balance: Int
    )

    data class DepositRequest(
        val amount: Int
    )


    @PostMapping()
    open fun create(@RequestBody body: AccountData): ResponseEntity<AccountData> {
        val id = data.size + 1
        data[id] = body.copy()
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<AccountData> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(path = ["/{id}/deposit"])
    open fun deposit(
        @PathVariable("id") id: Int,
        @RequestBody body: DepositRequest
    ): ResponseEntity<AccountData> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        // wrong: PUT must be idempotent, but each call accumulates the deposit
        resource.balance += body.amount

        return ResponseEntity.status(200).body(resource)
    }
}
