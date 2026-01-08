package com.foo.rest.examples.spring.openapi.v3.sleep

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import other.bar.sleep.SleepCounter

@RestController
@RequestMapping(path = ["/api/sleep"])
class SleepRest {


    @GetMapping
    open fun sleep() : ResponseEntity<String> {

        try {
            Thread.sleep(1_000_000_000)
        }catch (e: InterruptedException){
            //do nothing
        }

        SleepCounter.counter.getAndIncrement()

        return  ResponseEntity.ok("OK")
    }
}