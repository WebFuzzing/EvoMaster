package com.foo.rest.examples.spring.openapi.v3.gson.mapdouble

import com.google.gson.Gson
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/gson/map"])
class MapDoubleGsonRest {

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun classOfT(@RequestBody json: String): ResponseEntity<String> {
        val map = Gson().fromJson(json, Map::class.java)
        val keyword: Double = map["keyword"]!! as Double

        return if (keyword == 0.5) {
            ResponseEntity.ok("Working")
        } else {
            ResponseEntity.badRequest().body("Failed")
        }
    }
}
