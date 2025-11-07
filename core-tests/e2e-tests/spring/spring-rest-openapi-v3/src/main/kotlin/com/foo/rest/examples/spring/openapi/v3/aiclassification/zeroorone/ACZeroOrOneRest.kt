package com.foo.rest.examples.spring.openapi.v3.aiclassification.zeroorone

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneDto
import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneEnum
import com.foo.rest.examples.spring.openapi.v3.aiclassification.onlyone.ACOnlyOneEnum
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/zeroorone"])
class ACZeroOrOneRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        val px = x != null
        val pz = z == true

        // ZeroOrOne(x,z=true)
        if(! ( (px&&!pz) || (!px&&pz) || (!px&&!pz) )){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACZeroOrOneDto) : ResponseEntity<String> {

        val pd = body.d == ACZeroOrOneEnum.HELLO
        val pf = body.f == ACZeroOrOneEnum.X

        // ZeroOrOne(d=HELLO,f=X) -> same as both !&& when just two variables
        if( pd && pf){
            return ResponseEntity.status(400).build()
        }

        if(body.a != true && body.b!=null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }


}