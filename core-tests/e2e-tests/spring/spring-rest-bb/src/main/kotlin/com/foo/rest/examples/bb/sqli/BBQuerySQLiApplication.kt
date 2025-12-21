package com.foo.rest.examples.bb.sqli

import io.swagger.v3.oas.annotations.Operation
import org.evomaster.e2etests.utils.CoveredTargets
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
@RequestMapping(path = ["/api/sqli/query"])
open class BBQuerySQLiApplication {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var userRepository: UserRepository

    private var connection: Connection? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBQuerySQLiApplication::class.java, *args)
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
     * Attack: GET /api/sqli/query/vulnerable?username=admin' OR (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--
     */
    @GetMapping("/vulnerable")
    @Operation(summary = "SQL Injection - Query Parameter")
    fun timeBasedQuery(@RequestParam username: String): ResponseEntity<String> {
        CoveredTargets.cover("sqli")

        return try {
            val stmt = connection?.createStatement()
            val rs = stmt?.executeQuery("SELECT COUNT(*) as cnt FROM users WHERE username = '$username'")
            val count = if (rs?.next() == true) rs.getInt("cnt") else 0
            ResponseEntity.ok("COUNT: $count")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("ERROR: ${e.message}")
        }
    }

}
