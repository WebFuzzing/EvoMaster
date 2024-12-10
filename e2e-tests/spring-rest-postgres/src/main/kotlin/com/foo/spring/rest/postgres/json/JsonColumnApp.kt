package com.foo.spring.rest.postgres.json

import com.foo.spring.rest.postgres.SwaggerConfiguration
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.sql.Connection
import javax.sql.DataSource

/**
 * Created by jgaleotti on 2-Sept-22.
 */
@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/json"])
open class JsonColumnApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(JsonColumnApp::class.java, *args)
        }

    }

    @Autowired
    private lateinit var dataSource: DataSource

    private lateinit var connection: Connection

    @GetMapping(path = ["/fromJson"])
    open fun getComplexJSONTypes(): ResponseEntity<Any> {

        if (!this::connection.isInitialized) {
            connection = dataSource.connection
        }
        val rs = connection.createStatement().executeQuery("select jsonColumn from JSONSchemaTable where dummyColumn >0")
        val status: Int
        if (rs.next()) {
            val jsonColumn = rs.getObject(1)
            status = try {
                val fromJsonDto = Gson().fromJson(jsonColumn.toString(), FooDto::class.java)
                if (fromJsonDto.x>0) {
                    200
                } else {
                    400
                }
            } catch (ex: JsonSyntaxException) {
                400
            }
        } else {
            status = 400
        }
        return ResponseEntity.status(status).build()
    }
}

