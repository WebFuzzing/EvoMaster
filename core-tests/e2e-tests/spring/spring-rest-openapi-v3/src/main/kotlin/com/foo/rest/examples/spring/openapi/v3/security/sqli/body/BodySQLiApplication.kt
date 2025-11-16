package com.foo.rest.examples.spring.openapi.v3.security.sqli.body

import com.foo.rest.examples.spring.openapi.v3.security.sqli.common.LoginDto
import com.foo.rest.examples.spring.openapi.v3.security.sqli.common.UserDto
import com.foo.rest.examples.spring.openapi.v3.security.sqli.common.UserEntity
import com.foo.rest.examples.spring.openapi.v3.security.sqli.common.UserRepository
import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Connection
import javax.annotation.PostConstruct
import javax.sql.DataSource


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/sqli/body"])
@RestController
@ComponentScan(basePackages = ["com.foo.rest.examples.spring.openapi.v3.security.sqli.common", "com.foo.rest.examples.spring.openapi.v3.security.sqli.body"])
@EnableJpaRepositories(basePackages = ["com.foo.rest.examples.spring.openapi.v3.security.sqli.common"])
@EntityScan(basePackages = ["com.foo.rest.examples.spring.openapi.v3.security.sqli.common"])
open class BodySQLiApplication {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var userRepository: UserRepository

    private var connection: Connection? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BodySQLiApplication::class.java, *args)
        }
    }

    @PostConstruct
    fun init() {
        connection = dataSource.connection
        initializeTestData()
    }

    private fun initializeTestData() {
        if (userRepository.count() == 0L) {
            userRepository.save(UserEntity(null, "admin", "admin123"))
            userRepository.save(UserEntity(null, "user1", "password1"))
        }
    }

    /**
     * Safe endpoint - No SQL Injection vulnerability
     */
    @GetMapping("/safe")
    @Operation(summary = "Safe Query - No SQL Injection")
    fun getSafeUsers(): ResponseEntity<List<UserDto>> {
        val users = userRepository.findAll().map { UserDto(it.id, it.username) }
        return ResponseEntity.ok(users)
    }

    /**
     * Attack: POST {"username":"admin' AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--","password":"x"}
     */
    @PostMapping("/vulnerable")
    @Operation(summary = "SQL Injection - Body Parameter")
    fun body(@RequestBody loginDto: LoginDto): ResponseEntity<String> {
        return try {
            val stmt = connection?.createStatement()
            val rs = stmt?.executeQuery("SELECT COUNT(*) as cnt FROM users WHERE username = '${loginDto.username}' AND password = '${loginDto.password}'")
            val count = if (rs?.next() == true) rs.getInt("cnt") else 0
            ResponseEntity.ok("MATCHED: $count")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("ERROR: ${e.message}")
        }
    }

}
