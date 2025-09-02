package com.foo.rest.examples.spring.openapi.v3.aiclassification.mixed

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneDto
import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneEnum
import com.foo.rest.examples.spring.openapi.v3.aiclassification.arithmetic.ACArithmeticDto
import com.foo.rest.examples.spring.openapi.v3.aiclassification.onlyone.ACOnlyOneEnum
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/mixed"])
class ACMixedRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: Int?,
        @RequestParam("y") y: Int?,
        @RequestParam("a" )a: Boolean?,
        @RequestParam("b" )b: Boolean?,
        @RequestParam("s") s: String?,
        @RequestParam("c") c: Boolean?,
        @RequestParam("d") d: ACMixedEnum?,
    ) : ResponseEntity<String> {

        if(x == null || y == null || x > y){
            return ResponseEntity.status(400).build()
        }

        if(a == true && b == true){
            return ResponseEntity.status(400).build()
        }

        if(s==null){
            return ResponseEntity.status(400).build()
        }

        if(c == true){
            if(d != ACMixedEnum.HELLO){
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.ok().body("OK")
    }


}