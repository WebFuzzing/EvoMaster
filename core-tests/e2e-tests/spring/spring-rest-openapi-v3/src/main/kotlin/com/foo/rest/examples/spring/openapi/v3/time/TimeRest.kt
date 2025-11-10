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

        //checking different offsets
        if(x.contains("Z") ){
            return ResponseEntity.ok("A")
        }
        // there are always 2 - before the T
        if(x.chars().filter{it == '-'.code}.count() == 3L ){
            return ResponseEntity.ok("B")
        }
        if(x.contains("+") ){
            return ResponseEntity.ok("C")
        }

        //this shouldn't be reachable
        return ResponseEntity.ok("D")
    }
}