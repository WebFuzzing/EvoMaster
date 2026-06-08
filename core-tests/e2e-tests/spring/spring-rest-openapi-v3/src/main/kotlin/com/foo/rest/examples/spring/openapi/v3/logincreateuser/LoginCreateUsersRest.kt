package com.foo.rest.examples.spring.openapi.v3.logincreateuser

import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse.UserDto
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/logincreateusers"])
class LoginCreateUsersRest {

    private val SECRET = "a complex secret - "

    private val users = mutableMapOf<String, CreateUserDto>()

    private val tokens = mutableMapOf<String, String>()


    @PostMapping("/users")
    fun createUser(@RequestBody user: CreateUserDto): ResponseEntity<Unit> {

        if(user.email == null || user.username == null || user.password == null) {
            return ResponseEntity.status(400).build()
        }

        if(!user.email!!.contains("@") || !user.email!!.contains(".")) {
            return ResponseEntity.status(400).build()
        }

        if(user.password != user.repeatPassword) {
            return ResponseEntity.status(400).build()
        }

        if(users.containsKey(user.email)){
            return ResponseEntity.status(403).build()
        }

        users[user.email!!] = user
        return ResponseEntity.status(201).build()
    }


    @PostMapping(path = ["/users/login"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@RequestBody login : LoginDto) : ResponseEntity<AuthDto>{

        val user = users[login.email!!]
            ?: return ResponseEntity.status(404).build()

        if(login.password != user.password){
            return ResponseEntity.status(400).build()
        }

        val secret = "$SECRET${System.currentTimeMillis()}"

        tokens[secret] = user.email!!

        return ResponseEntity.ok(AuthDto(user.email, TokenDto(secret)))
    }

    @GetMapping(path = ["/check"])
    fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        val secret = authorization!!.substring("Bearer ".length)

        if(tokens.containsKey(secret)){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(401).build()
    }
}
