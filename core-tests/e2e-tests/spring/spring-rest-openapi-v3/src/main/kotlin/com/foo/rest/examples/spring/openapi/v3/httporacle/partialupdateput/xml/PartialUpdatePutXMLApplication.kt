package com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput.xml

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
open class PartialUpdatePutXMLApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PartialUpdatePutXMLApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, ResourceData>()

        fun reset(){
            data.clear()
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


    @PostMapping(
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    open fun create(@RequestBody body: ResourceData): ResponseEntity<ResourceData> {
        val id = data.size + 1
        data[id] = ResourceData(name = body.name, value = body.value)
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping(
        path = ["/{id}"],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    open fun get(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(
        path = ["/{id}"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    open fun put(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        if(body.name != null) {
            resource.name = body.name
        }

        return ResponseEntity.status(200).body(resource)
    }
}