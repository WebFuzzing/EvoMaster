package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.array

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
@RequestMapping(path = ["/api/carts"])
@RestController
open class HttpNonIdempotentPutArrayApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpNonIdempotentPutArrayApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, CartData>()

        fun reset(){
            data.clear()
        }
    }

    data class CartData(
        var items: MutableList<String>
    )

    data class AddItemRequest(
        val item: String
    )


    @PostMapping()
    open fun create(@RequestBody body: CartData): ResponseEntity<CartData> {
        val id = data.size + 1
        data[id] = body.copy(items = body.items.toMutableList())
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<CartData> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(path = ["/{id}/items"])
    open fun addItem(
        @PathVariable("id") id: Int,
        @RequestBody body: AddItemRequest
    ): ResponseEntity<CartData> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        // wrong: PUT must be idempotent (full replace), but each call appends an item
        resource.items.add(body.item)

        return ResponseEntity.status(200).body(resource)
    }
}
