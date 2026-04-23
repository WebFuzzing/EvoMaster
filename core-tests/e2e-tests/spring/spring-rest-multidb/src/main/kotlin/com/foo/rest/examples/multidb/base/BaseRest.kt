package com.foo.rest.examples.multidb.base

import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/base"])
@RestController
open class BaseRest(
    private val jdbc: JdbcTemplate
) {

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable id: String) : ResponseEntity<String> {


        val x = jdbc.query("SELECT * FROM foo.BaseTable WHERE id = '$id'", BeanPropertyRowMapper(BaseTable::class.java))

        if(x.isNotEmpty()) {
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.notFound().build()
    }
}