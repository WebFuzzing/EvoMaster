package com.foo.rest.examples.spring.openapi.v3.aiclassification.arithmetic

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/arithmetic"])
class ACArithmeticRest {

    @GetMapping
    open fun get(
        @RequestParam("x") x: Int?,
        @RequestParam("y") y: Int?,
        @RequestParam("z") z: Double?,
        @RequestParam("k") k: Double?,
        @RequestParam("s") s: String?,
    ): ResponseEntity<String> {

        // Check if required parameters are present
        if (x == null || y == null) {
            return ResponseEntity.badRequest().build()
        }

        // Compare x and y
        if (x >= y) {
            return ResponseEntity.status(400).build()
        }

        // Check z and k comparison only if both are present
        if (z != null && k != null && z < k) {
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body: ACArithmeticDto): ResponseEntity<String> {
        // Validate required fields
        if (body.c == null || body.e == null || body.f == null || body.g == null) {
            return ResponseEntity.badRequest().build()
        }

        if (body.c == body.e || body.f!! < body.g!!) {
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }
}