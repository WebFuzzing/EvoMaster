package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.urlencoded

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.beans.PropertyEditorSupport

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class UrlencodedFailModificationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(UrlencodedFailModificationApplication::class.java, *args)
        }

        private val dataAlreadyExists = mutableMapOf<Int, ResourceData>()

        fun reset(){
            dataAlreadyExists.clear()
            dataAlreadyExists[0] = ResourceData("existing", 42)
        }
    }

    open class ResourceData(
        var name: String = "",
        var value: Int = 0
    )

    open class UpdateRequest(
        var name: String = "",
        var value: Int = 0
    )


    @PostMapping(path = ["/notempty"], consumes = ["application/x-www-form-urlencoded"], produces = ["application/json"])
    open fun createnotempty(@ModelAttribute body: ResourceData): ResponseEntity<ResourceData> {
        val id = dataAlreadyExists.size + 1
        dataAlreadyExists[id] = ResourceData(body.name, body.value)
        return ResponseEntity.status(201).body(dataAlreadyExists[id])
    }

    @GetMapping(path = ["/notempty/{id}"], produces = ["application/json"])
    open fun getnotempty(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = dataAlreadyExists[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(path = ["/notempty/{id}"], consumes = ["application/x-www-form-urlencoded"], produces = ["text/plain"])
    open fun putnotempty(
        @PathVariable("id") id: Int,
        @ModelAttribute body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = dataAlreadyExists[id]
            ?: return ResponseEntity.status(404).build()

        resource.name = body.name
        resource.value = body.value

        // returns 400 Bad Request, but the data was already modified above
        return ResponseEntity.status(400).body("Invalid request")
    }

    @PatchMapping(path = ["/notempty/{id}"], consumes = ["application/x-www-form-urlencoded"], produces = ["text/plain"])
    open fun patchnotempty(
        @PathVariable("id") id: Int,
        @ModelAttribute body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = dataAlreadyExists[id]
            ?: return ResponseEntity.status(404).build()

        // correct: validation first, reject without modifying
        return ResponseEntity.status(400).body("No fields to update")

        // correct: does NOT modify data, just returns 4xx
        return ResponseEntity.status(403).body("Forbidden")
    }
}
