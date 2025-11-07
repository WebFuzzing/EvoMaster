package com.foo.rest.examples.spring.openapi.v3.authenticatedswaggerendpoint

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class AuthenticatedSwaggerEndpointRest {

    private val SECRET = "Secret Authentication Bearer..."

    // OD - Note, this path will work for e2e test but not for manual test
    // For manual testing, the working directory should be set to ./e2e-tests/spring-rest-openapi-v3/
    private val SWAGGER_PATH = "./static/authenticatedSwaggerEndpointOpenAPIv3.json"

    private val swaggerJSONText = this::class.java.classLoader.getResource(SWAGGER_PATH)?.readText()


    @PostMapping(path = ["/api/logintoken/login"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@RequestBody login : LoginDto) : ResponseEntity<AuthDto>{

        if(login.userId == "userName1" && login.password == "password1234"){
            return ResponseEntity.ok(AuthDto("foo", TokenDto(SECRET)))
        }

        return ResponseEntity.status(400).build()
    }

    @GetMapping(path = ["/api/logintoken/check"])
    fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        if(authorization == "Bearer $SECRET"){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(401).build()
    }

    @GetMapping(path = ["/openapi-schema"])
    fun retrieveAPIDocs(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        if(authorization == "Bearer $SECRET"){

            return ResponseEntity.ok(swaggerJSONText)
        }

        return ResponseEntity.status(401).build()

    }



}