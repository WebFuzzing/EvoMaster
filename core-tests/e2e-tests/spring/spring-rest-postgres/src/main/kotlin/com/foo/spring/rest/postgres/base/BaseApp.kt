package com.foo.spring.rest.postgres.base

import com.foo.spring.rest.postgres.SwaggerConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import springfox.documentation.swagger2.annotations.EnableSwagger2
import javax.persistence.EntityManager




/**
 * Created by arcuri82 on 21-Jun-19.
 */
@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/basic"])
open class BaseApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BaseApp::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em : EntityManager


    @GetMapping
    open fun get() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select * from X where id>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build<Any>()
    }
}
