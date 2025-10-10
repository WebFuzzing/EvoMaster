package com.foo.rest.examples.spring.openapi.v3.aiclassification.imply

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneDto
import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneEnum
import com.foo.rest.examples.spring.openapi.v3.aiclassification.onlyone.ACOnlyOneEnum
import com.foo.rest.examples.spring.openapi.v3.aiclassification.zeroorone.ACZeroOrOneDto
import com.foo.rest.examples.spring.openapi.v3.aiclassification.zeroorone.ACZeroOrOneEnum
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/imply"])
class ACImplyRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        if(z == true){
            if(x == null) {
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACImplyDto) : ResponseEntity<String> {

        if(body.a == true){
            if(! (body.d == ACImplyEnum.HELLO || body.f == ACImplyEnum.HELLO)){
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.status(201).body("OK")
    }


}