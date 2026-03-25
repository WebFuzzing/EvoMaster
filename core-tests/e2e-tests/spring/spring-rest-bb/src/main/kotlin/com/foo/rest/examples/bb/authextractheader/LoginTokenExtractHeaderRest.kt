package com.foo.rest.examples.bb.authextractheader

import com.foo.rest.examples.bb.authtoken.AuthDto
import com.foo.rest.examples.bb.authtoken.LoginDto
import com.foo.rest.examples.bb.authtoken.TokenDto
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/logintokenextractheader"])
class LoginTokenExtractHeaderRest {

    private val SECRET = "a complex secret..."

    @PostMapping(path = ["/login"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun login(@RequestBody login : LoginDto) : ResponseEntity<*>{

        if(login.userId == "foo" && login.password == "123"){
            return ResponseEntity.ok().header("X-Auth-Token", SECRET).build<Any>()
        }

        return ResponseEntity.status(400).build<Any>()
    }

    @GetMapping(path = ["/check"])
    open fun check(@RequestHeader("Authorization") authorization: String?) : ResponseEntity<String>{

        if(authorization == "Bearer $SECRET"){
            CoveredTargets.cover("OK")
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(401).build()
    }
}