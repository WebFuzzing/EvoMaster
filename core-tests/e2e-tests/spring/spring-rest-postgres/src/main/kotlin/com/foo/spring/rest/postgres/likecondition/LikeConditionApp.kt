package com.foo.spring.rest.postgres.likecondition

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

@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/like"])
open class LikeConditionApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(LikeConditionApp::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em : EntityManager


    @GetMapping
    open fun get() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select * from LikeConditionTable where name LIKE 'foo%'")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build<Any>()
    }
}