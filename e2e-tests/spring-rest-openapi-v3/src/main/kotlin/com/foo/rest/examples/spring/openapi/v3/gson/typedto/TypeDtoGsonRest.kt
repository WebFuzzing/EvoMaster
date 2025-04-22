package com.foo.rest.examples.spring.openapi.v3.gson.typedto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/gson/type"])
class TypeDtoGsonRest {

    class TestDto {
        var value: Double = 0.0
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun classOfT(@RequestBody json: String): ResponseEntity<String> {
        val arrayList : ArrayList<TestDto> = Gson().fromJson(json, object : TypeToken<ArrayList<TestDto>>() {}.type)
        val dto : TestDto = arrayList[1]

        return if (dto.value > 0) {
            ResponseEntity.ok("Working")
        } else {
            ResponseEntity.badRequest().body("Failed")
        }
    }
}
