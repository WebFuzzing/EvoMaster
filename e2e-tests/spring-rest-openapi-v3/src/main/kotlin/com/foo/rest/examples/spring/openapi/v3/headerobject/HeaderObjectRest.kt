package com.foo.rest.examples.spring.openapi.v3.headerobject

import com.google.gson.Gson
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/headerobject"])
open class HeaderObjectRest {

    @GetMapping
//    open fun get(@RequestHeader token: Token) : ResponseEntity<String> {
    open fun get(@RequestHeader token: String) : ResponseEntity<String> {

        val _token = Gson().fromJson<Token>(token, Token::class.java)

        if(_token.counter > 0 && _token.x.length > 0) {
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}



class Token{
    var counter : Int = 0
    var x : String = ""
}
