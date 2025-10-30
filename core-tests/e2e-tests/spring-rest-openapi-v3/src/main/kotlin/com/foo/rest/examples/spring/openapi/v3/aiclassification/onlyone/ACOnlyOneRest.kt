package com.foo.rest.examples.spring.openapi.v3.aiclassification.onlyone

import com.foo.rest.examples.spring.openapi.v3.aiclassification.required.ACRequiredDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/onlyone"])
class ACOnlyOneRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        val px = x != null
        val pz = z == true

        // OnlyOne(x,z=true)
        if(! ( (px&&!pz) || (!px&&pz))){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACOnlyOneDto) : ResponseEntity<String> {

        val pa = body.a == false
        val pd = body.d == ACOnlyOneEnum.FOO

        // OnlyOne(a=false,d=FOO)
        if(! ((pa&&!pd) || (!pa&&pd))){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }


}