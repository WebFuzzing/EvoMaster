package com.foo.rest.examples.spring.openapi.v3.httporaclefp.partialupdateputfp

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
import java.net.URI

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class PartialUpdatePutFPApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PartialUpdatePutFPApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, ResourceData>()

        fun reset(){
            data.clear()
        }
    }

    //the actual stored data has 4 fields
    data class ResourceData(
        var name: String,
        var value: Int,
        var timestamp: Long,
        var hidden: String
    )

    // GET does not allow to read hidden
    data class GetRequest(
        var name: String,
        var value: Int,
        var timestamp: Long
    )

    // creation/update does not allow to set the timestamp
    data class CreateUpdateRequest(
        val name: String,
        val value: Int,
        val hidden: String
    )


    @PostMapping()
    open fun create(@RequestBody body: CreateUpdateRequest): ResponseEntity<ResourceData> {
        val id = data.size + 1
        data[id] = ResourceData(name = body.name, value = body.value, hidden = body.hidden, timestamp = System.currentTimeMillis())
        return ResponseEntity.created(URI("/api/resources/$id")).body(data[id])
    }

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<GetRequest> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        //here "hidden" cannot be returned
        return ResponseEntity.status(200).body(
            GetRequest(name = resource.name, value = resource.value, timestamp = System.currentTimeMillis())
        )
    }

    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int,
        @RequestBody body: CreateUpdateRequest
    ): ResponseEntity<Any> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        //here doing a full replacement
        resource.value = body.value
        resource.name = body.name
        resource.hidden = body.hidden
        //this is server-side generated
        resource.timestamp = System.currentTimeMillis()

        return ResponseEntity.status(200).build()
    }
}