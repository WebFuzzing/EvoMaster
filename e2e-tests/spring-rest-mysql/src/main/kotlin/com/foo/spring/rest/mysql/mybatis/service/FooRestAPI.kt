package com.foo.spring.rest.mysql.mybatis.service

import com.foo.spring.rest.mysql.mybatis.mapper.FooMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api"])
open class FooRestAPI {

    @Autowired
    private lateinit var mapper : FooMapper


    @GetMapping("/foo")
    open fun get(@RequestParam("name") name: String) : ResponseEntity<Any> {

        val res = mapper.findByName(name)

        val status = if(res == null) 400 else 200

        return ResponseEntity.status(status).build<Any>()
    }
}