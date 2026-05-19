package com.foo.spring.rest.postgres.multicolumnfk

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
@RequestMapping(path = ["/api/postgres/multicolumnfk"])
open class PostgresMultiColumnFKApplication : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PostgresMultiColumnFKApplication::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em: EntityManager

    @GetMapping(path = ["/testFK"])
    open fun testFK(): ResponseEntity<Any> {
        val query = em.createNativeQuery("select * from Parent p JOIN Child c ON p.id1 = c.parent_id1 AND p.id2 = c.parent_id2 WHERE c.parent_id1>0 AND c.parent_id2>0")
        val res = query.resultList

        val status = if (res.isNotEmpty()) 200 else 400
        return ResponseEntity.status(status).build()
    }
}
