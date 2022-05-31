package com.foo.spring.rest.mysql.datatypes

import com.foo.spring.rest.mysql.SwaggerConfiguration
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
@RequestMapping(path = ["/api/mysql"])
open class MySqlDataTypesApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MySqlDataTypesApp::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em : EntityManager


    @GetMapping(path = ["/integerdatatypes"])
    open fun getIntegerDataTypes() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from integerdatatypes where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }


    @GetMapping(path = ["/floatingpointtypes"])
    open fun getFloatingPointTypes() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from floatingpointtypes where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/bitdatatype"])
    open fun getBitDataType() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from bitdatatype where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/booleandatatypes"])
    open fun getBooleanDataTypes() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from booleandatatypes where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/serialdatatype"])
    open fun getSerialDataType() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from serialdatatype where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/dateandtimetypes"])
    open fun getDateAndTimeTypes() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from dateandtimetypes where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/stringdatatypes"])
    open fun getStringDataTypes() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from stringdatatypes where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

//    @GetMapping(path = ["/fixedpointtypes"])
    open fun getFixedPointTypes() : ResponseEntity<Any> {

        val query = em.createNativeQuery("select (1) from fixedpointtypes where integercolumn>0")
        val res = query.resultList

        val status = if(res.isEmpty()) 400 else 200

        return ResponseEntity.status(status).build()
    }

}