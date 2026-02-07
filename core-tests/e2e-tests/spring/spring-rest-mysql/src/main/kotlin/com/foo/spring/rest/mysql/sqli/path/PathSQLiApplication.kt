package com.foo.spring.rest.mysql.sqli.path


import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.sql.Connection
import javax.annotation.PostConstruct
import javax.sql.DataSource


@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/sqli/path"])
@RestController
open class PathSQLiApplication {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var userRepository: UserRepository

    private var connection: Connection? = null

    companion object {
        @Autowired
        private lateinit var userRepository: UserRepository

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PathSQLiApplication::class.java, *args)
        }


        fun reset() {
            userRepository.deleteAll()
            userRepository.save(UserEntity(null, "admin", "admin123"))
            userRepository.save(UserEntity(null, "user1", "password1"))
        }
    }

    @PostConstruct
    fun init() {
        connection = dataSource.connection
        Companion.userRepository = this.userRepository
        reset()
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
     * Attack: GET /api/sqli/path/vulnerable/admin' OR (SELECT SUM(a.ORDINAL_POSITION*b.ORDINAL_POSITION*c.ORDINAL_POSITION) FROM INFORMATION_SCHEMA.COLUMNS a, INFORMATION_SCHEMA.COLUMNS b, INFORMATION_SCHEMA.COLUMNS c)>1 --
     */
    @GetMapping("/vulnerable/{id}")
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

}
