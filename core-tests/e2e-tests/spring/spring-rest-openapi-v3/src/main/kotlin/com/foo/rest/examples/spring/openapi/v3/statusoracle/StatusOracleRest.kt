package com.foo.rest.examples.spring.openapi.v3.statusoracle

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.ws.rs.Consumes


@RestController
@RequestMapping(path = ["/api/statusoracle"])
private class StatisticsRest {

    @GetMapping(path = ["/no-non-standard-codes/42"])
    fun noNonStandardCodes42() : ResponseEntity<String> {
        return ResponseEntity.status(42).build()
    }

    @GetMapping(path = ["/no-non-standard-codes/912"])
    fun noNonStandardCodes912() : ResponseEntity<String> {
        return ResponseEntity.status(912).build()
    }

    @GetMapping(path = ["/no-non-standard-codes/1024"])
    fun noNonStandardCodes1024() : ResponseEntity<String> {
        return ResponseEntity.status(1024).build()
    }

    @DeleteMapping(path = ["/no-201-if-delete"])
    fun no201IfDelete(): ResponseEntity<String> {
        return ResponseEntity.status(201).build()
    }

    @GetMapping(path = ["/no-201-if-get"])
    fun no201IfGet(): ResponseEntity<String> {
        return ResponseEntity.status(201).build()
    }

    @PatchMapping(path = ["/no-201-if-patch"])
    fun no201IfPatch(@RequestBody x: String): ResponseEntity<String> {
        return ResponseEntity.status(201).build()
    }

    @GetMapping(path = ["/no-204-if-content"])
    fun no204IfContent(): ResponseEntity<String> {
        return ResponseEntity.status(204).body("Hello")
    }

    @PostMapping(path = ["/no-413-if-no-payload"])
    fun no413IfNoPayload() : ResponseEntity<String> {
        return ResponseEntity.status(413).build()
    }

    @PostMapping(path = ["/no-415-if-no-payload"])
    fun no415IfNoPayload() : ResponseEntity<String> {
        return ResponseEntity.status(415).build()
    }

    class SODto{var x : Int? = null}

    @Consumes("application/json")
    @PostMapping(path = ["/has-406-if-accept"])
    fun has406IfAccept(@RequestBody dto: SODto) : ResponseEntity<String> {
        return ResponseEntity.status(406).build()
    }

    @GetMapping(path = ["/no-401-if-no-auth"])
    fun no401IfNoAuth(): ResponseEntity<String>{
        return ResponseEntity.status(401).build()
    }

    @GetMapping(path = ["/no-403-if-no-401"])
    fun no403IfNo401(): ResponseEntity<String>{
        return ResponseEntity.status(403).build()
    }

}
