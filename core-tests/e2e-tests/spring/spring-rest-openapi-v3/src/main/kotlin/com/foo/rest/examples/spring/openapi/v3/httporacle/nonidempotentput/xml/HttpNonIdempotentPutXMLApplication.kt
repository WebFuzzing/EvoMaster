package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.xml

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
@RequestMapping(path = ["/api/accounts"])
@RestController
open class HttpNonIdempotentPutXMLApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpNonIdempotentPutXMLApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, AccountData>()

        fun reset(){
            data.clear()
        }
    }

    @XmlRootElement(name = "accountData")
    @XmlAccessorType(XmlAccessType.FIELD)
    open class AccountData(
        var balance: Int = 0
    )

    @XmlRootElement(name = "depositRequest")
    @XmlAccessorType(XmlAccessType.FIELD)
    open class DepositRequest(
        var amount: Int = 0,
        // EvoMaster's ObjectGene XML serializer inlines single-field bodies as
        // <depositRequest>5</depositRequest> instead of <depositRequest><amount>5</amount></depositRequest>,
        // which JAXB cannot bind (amount falls back to its Kotlin default).
        // This unused field forces the multi-field path so the random `amount` actually reaches the server.
        var note: String = ""
    )


    @PostMapping(
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    open fun create(@RequestBody body: AccountData): ResponseEntity<AccountData> {
        val id = data.size + 1
        data[id] = AccountData(balance = body.balance)
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping(
        path = ["/{id}"],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    open fun get(@PathVariable("id") id: Int): ResponseEntity<AccountData> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(
        path = ["/{id}/deposit"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
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
