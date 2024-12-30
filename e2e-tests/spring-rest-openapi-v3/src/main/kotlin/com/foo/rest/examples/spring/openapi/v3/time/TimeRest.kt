package com.foo.rest.examples.spring.openapi.v3.time

import com.ethlo.time.ITU
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/time"])
class TimeRest {


    @GetMapping
    open fun get(@RequestParam x: String) : ResponseEntity<String> {

        try{
            ITU.parseDateTime(x)
        }catch (e:Exception){
            return ResponseEntity.badRequest().body(e.message)
        }

        if(x.contains("Z") ){
            return ResponseEntity.ok("A")
        }
        if(x.contains("-") ){
            return ResponseEntity.ok("B")
        }
        if(x.contains("+") ){
            return ResponseEntity.ok("C")
        }

        return ResponseEntity.ok("D")
    }
}