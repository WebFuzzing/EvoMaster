package com.foo.rest.examples.multidb.separatedschemas

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/separatedschemas"])
@RestController
open class SeparatedSchemasRest(
    private val repositoryX: RepositoryX,
    private val repositoryY: RepositoryY
) {

    @GetMapping(path = ["/x/{id}"])
    open fun getX(@PathVariable id: String) : ResponseEntity<String> {

        val found = repositoryX.findById(id)
        if(found.isPresent) {
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.notFound().build()
    }

    @GetMapping(path = ["/y/{id}"])
    open fun getY(@PathVariable id: String) : ResponseEntity<String> {

        val found = repositoryY.findById(id)
        if(found.isPresent) {
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.notFound().build()
    }

}