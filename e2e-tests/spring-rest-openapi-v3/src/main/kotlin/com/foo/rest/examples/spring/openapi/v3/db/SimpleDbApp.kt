package com.foo.rest.examples.spring.openapi.v3.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.persistence.EntityManager

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/simpledb"])
class SimpleDbApp {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SimpleDbApp::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em: EntityManager


    @GetMapping()
    fun get(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from IntegerTypes where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }


}