package com.foo.rest.examples.spring.openapi.v3.security.sqli

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Connection
import javax.annotation.PostConstruct
import javax.sql.DataSource


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/sqli"])
@RestController
open class SQLiApplication {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var userRepository: UserRepository

    private var connection: Connection? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SQLiApplication::class.java, *args)
        }
    }

    @PostConstruct
    fun init() {
        connection = dataSource.connection
        initializeTestData()
    }

    private fun initializeTestData() {
        if (userRepository.count() == 0L) {
            userRepository.save(UserEntity(null,"admin", "admin123"))
            userRepository.save(UserEntity(null,"user1","password1"))
        }
    }

    /**
     * Attack: GET /api/sqli/query?username=admin' OR (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--
     */
    @GetMapping("/query")
    @Operation(summary = "SQL Injection - Query Parameter")
    fun timeBasedQuery(@RequestParam username: String): ResponseEntity<String> {
        return try {
            val stmt = connection?.createStatement()
            val rs = stmt?.executeQuery("SELECT COUNT(*) as cnt FROM users WHERE username = '$username'")
            val count = if (rs?.next() == true) rs.getInt("cnt") else 0
            ResponseEntity.ok("COUNT: $count")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("ERROR: ${e.message}")
        }
    }

    /**
     * Attack: GET /api/sqli/path/admin' OR (SELECT SUM(a.ORDINAL_POSITION*b.ORDINAL_POSITION*c.ORDINAL_POSITION) FROM INFORMATION_SCHEMA.COLUMNS a, INFORMATION_SCHEMA.COLUMNS b, INFORMATION_SCHEMA.COLUMNS c)>1 --
     */
    @GetMapping("/path/{id}")
    @Operation(summary = "SQL Injection - Path Parameter")
    fun timeBasedPath(@PathVariable id: String): ResponseEntity<String> {
        return try {
            val stmt = connection?.createStatement()
            val rs = stmt?.executeQuery("SELECT username FROM users WHERE username = '$id'")
            val username = if (rs?.next() == true) rs.getString("username") else "NOT_FOUND"
            ResponseEntity.ok("USERNAME: $username")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("ERROR: ${e.message}")
        }
    }

    /**
     * Attack: POST {"username":"admin' AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--","password":"x"}
     */
    @PostMapping("/body")
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
