package com.foo.spring.rest.postgres.columntypes

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
open class PostgresColumnTypesApplication : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PostgresColumnTypesApplication::class.java, *args)
        }
    }

    @Autowired
    private lateinit var em: EntityManager


    @GetMapping(path = ["/integerTypes"])
    open fun getIntegerTypes(): ResponseEntity<Any> {

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

    @GetMapping(path = ["/arbitraryPrecisionNumbers"])
    open fun getArbitraryPrecisionNumbers(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from ArbitraryPrecisionNumbers where dummyColumn>0")
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

        val query = em.createNativeQuery("select 1 from FloatingPointTypes where dummyColumn>0")
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

        val query = em.createNativeQuery("select 1 from MonetaryTypes where dummyColumn>0")
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

        val query = em.createNativeQuery("select 1 from CharacterTypes where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/binaryDataTypes"])
    open fun getBinaryDataTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from BinaryDataTypes  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/bitStringTypes"])
    open fun getBitStringTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from BitStringTypes  where dummyColumn>0 ")
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

        val query = em.createNativeQuery("select 1 from BooleanType where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/geometricTypes"])
    open fun getGeometricTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from GeometricTypes  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/dateTimeTypes"])
    open fun getDateTimeTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from DateTimeTypes  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/serialTypes"])
    open fun getSerialTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from SerialTypes  where dummyColumn>0 ")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }


    @GetMapping(path = ["/networkAddressTypes"])
    open fun getNetworkAddressTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from NetworkAddressTypes where dummyColumn>0 ")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/textSearchTypes"])
    open fun getTextSearchTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from TextSearchTypes where dummyColumn>0 ")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/uuidType"])
    open fun getUUIDType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from UUIDType where dummyColumn>0 ")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/xmlType"])
    open fun getXMLType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from XMLType  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/jsonTypes"])
    open fun getJSONTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from JSONTypes  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/builtInRangeTypes"])
    open fun getBuiltInRangeTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from BuiltInRangeTypes  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/enumType"])
    open fun getEnumType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from TableUsingEnumType  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/compositeType"])
    open fun getCompositeType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from TableUsingCompositeType  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/nestedCompositeType"])
    open fun getNestedCompositeType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from TableUsingNestedCompositeType where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/arrayTypes"])
    open fun getArrayTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from ArrayTypes where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/pglsnType"])
    open fun getPglsnType(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from PgLsnType where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/builtInMultiRangeTypes"])
    open fun getBuiltInMultiRangeTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from MultiRangeBuiltInTypes  where dummyColumn>0")
        val res = query.resultList

        val status: Int
        if (res.isNotEmpty()) {
            status = 200
        } else {
            status = 400
        }

        return ResponseEntity.status(status).build()
    }

    @GetMapping(path = ["/objectIdentifierTypes"])
    open fun getObjectIdentifierTypes(): ResponseEntity<Any> {

        val query = em.createNativeQuery("select 1 from ObjectIdentifierTypes  where dummyColumn>0")
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

