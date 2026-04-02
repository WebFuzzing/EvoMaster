package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.xml

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class XmlFailModificationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XmlFailModificationApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, ResourceData>()
        private val dataAlreadyExists = mutableMapOf<Int, ResourceData>()

        fun reset(){
            data.clear()
            dataAlreadyExists.clear()
            dataAlreadyExists[0] = ResourceData("existing", 42)
        }
    }

    @XmlRootElement(name = "resourceData")
    @XmlAccessorType(XmlAccessType.FIELD)
    open class ResourceData(
        var name: String = "",
        var value: Int = 0
    )

    @XmlRootElement(name = "updateRequest")
    @XmlAccessorType(XmlAccessType.FIELD)
    open class UpdateRequest(
        var name: String = "",
        var value: Int = 0
    )


    @PostMapping(path = ["/empty"], consumes = ["application/xml"], produces = ["application/xml"])
    open fun create(@RequestBody body: ResourceData): ResponseEntity<ResourceData> {
        val id = data.size + 1
        data[id] = ResourceData(body.name, body.value)
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping(path = ["/empty/{id}"], produces = ["application/xml"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(path = ["/empty/{id}"], consumes = ["application/xml"], produces = ["text/plain"])
    open fun put(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        // bug: modifies data even though it will return 4xx
        if(body.name != null) {
            resource.name = body.name
        }
        if(body.value != null) {
            resource.value = body.value
        }

        // returns 400 Bad Request, but the data was already modified above
        return ResponseEntity.status(400).body("Invalid request")
    }

    @PatchMapping(path = ["/empty/{id}"], consumes = ["application/xml"], produces = ["text/plain"])
    open fun patch(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        // correct: validation first, reject without modifying
        if(body.name == null && body.value == null) {
            return ResponseEntity.status(400).body("No fields to update")
        }

        // correct: does NOT modify data, just returns 4xx
        return ResponseEntity.status(403).body("Forbidden")
    }

    // pre-populated resource to test that it is not modified by failed PUT

    @PostMapping(path = ["/notempty"], consumes = ["application/xml"], produces = ["application/xml"])
    open fun createnotempty(@RequestBody body: ResourceData): ResponseEntity<ResourceData> {
        val id = dataAlreadyExists.size + 1
        dataAlreadyExists[id] = ResourceData(body.name, body.value)
        return ResponseEntity.status(201).body(dataAlreadyExists[id])
    }

    @GetMapping(path = ["/notempty/{id}"], produces = ["application/xml"])
    open fun getnotempty(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = dataAlreadyExists[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(path = ["/notempty/{id}"], consumes = ["application/xml"], produces = ["text/plain"])
    open fun putnotempty(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = dataAlreadyExists[id]
            ?: return ResponseEntity.status(404).build()

        resource.name = body.name
        resource.value = body.value

        // returns 400 Bad Request, but the data was already modified above
        return ResponseEntity.status(400).body("Invalid request")
    }

    @PatchMapping(path = ["/notempty/{id}"], consumes = ["application/xml"], produces = ["text/plain"])
    open fun patchnotempty(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = dataAlreadyExists[id]
            ?: return ResponseEntity.status(404).build()

        // correct: validation first, reject without modifying
        return ResponseEntity.status(400).body("No fields to update")

        // correct: does NOT modify data, just returns 4xx
        return ResponseEntity.status(403).body("Forbidden")
    }
}
