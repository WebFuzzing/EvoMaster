package com.foo.rest.examples.multidb.base

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/base"])
@RestController
open class BaseRest(
    private val repository: BaseRepository
) {

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable id: String) : ResponseEntity<String> {

        val found = repository.findById(id)
        if(found.isPresent) {
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.notFound().build()
    }
}