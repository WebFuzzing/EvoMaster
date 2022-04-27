package com.foo.spring.rest.postgres.dbapp

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
 * Created by jgaleotti on 18-Apr-22.
 */
@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/postgres"])
open class DbApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DbApp::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em: EntityManager


    @GetMapping(path = ["/integerTypes"])
    open fun getIntegerTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from IntegerTypes where integerColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/arbitraryPrecisionNumbers"])
    open fun getArbitraryPrecisionNumbers(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from ArbitraryPrecisionNumbers where numericColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/floatingPointTypes"])
    open fun getFloatingPointTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from FloatingPointTypes where realColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/monetaryTypes"])
    open fun getMonetaryTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from MonetaryTypes where moneyColumn>'0'")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/characterTypes"])
    open fun getChracterTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from CharacterTypes where varcharColunmn!=''")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    //@GetMapping(path = ["/binaryDataTypes"])
    open fun getBinaryDataTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from BinaryDataTypes where byteaColumn!=''")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    //@GetMapping(path = ["/bitStringTypes"])
    open fun getBitStringTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from BitStringTypes where bitColumn!=B''")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }


    @GetMapping(path = ["/booleanType"])
    open fun getBooleanType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from BooleanType where booleanColumn!='true'")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    //@GetMapping(path = ["/geometricTypes"])
    open fun getGeometricTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from GeometricTypes where pointColumn!='(0,0)'")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    //@GetMapping(path = ["/dateTimeTypes"])
    open fun getDateTimeTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from DateTimeTypes where timestampColumn!='1999-01-08 04:05:06'")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    //@GetMapping(path = ["/serialTypes"])
    open fun getSerialTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from SerialTypes where serialColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }


    //@GetMapping(path = ["/networkAddressTypes"])
    open fun getNetworkAddressTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from NetworkAddressTypes where macaddrColumn!='08:00:2b:01:02:03'")
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

