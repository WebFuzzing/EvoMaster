package com.foo.spring.rest.postgres.sqli.body

import com.foo.spring.rest.postgres.SwaggerConfiguration
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@RequestMapping(path = ["/api/sqli/body"])
@RestController
open class BodySQLiApplication: SwaggerConfiguration() {

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
    }

    /**
     * Safe endpoint - No SQL Injection vulnerability
     */
    @GetMapping("/safe")
    @Operation(summary = "Safe Query - No SQL Injection")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful operation",
            content = [Content(mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = UserDto::class)))])
    ])
    fun getSafeUsers(): ResponseEntity<List<UserDto>> {
        val users = userRepository.findAll().map { UserDto(it.id, it.username) }
        return ResponseEntity.ok(users)
    }

    /**
     * Attack: POST {"username":"admin' AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--","password":"x"}
     */
    @PostMapping("/vulnerable")
    @Operation(summary = "SQL Injection - Body Parameter")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successful operation",
            content = [Content(mediaType = "text/plain", schema = Schema(implementation = String::class))]),
        ApiResponse(responseCode = "500", description = "Internal server error",
            content = [Content(mediaType = "text/plain", schema = Schema(implementation = String::class))])
    ])
    @RequestBody(description = "Login credentials", required = true,
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = LoginDto::class))])
    fun body(@org.springframework.web.bind.annotation.RequestBody loginDto: LoginDto): ResponseEntity<String> {
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
