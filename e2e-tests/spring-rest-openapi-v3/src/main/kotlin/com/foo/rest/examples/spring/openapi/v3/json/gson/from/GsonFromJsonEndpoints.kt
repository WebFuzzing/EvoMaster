package com.foo.rest.examples.spring.openapi.v3.json.gson.from

import com.google.gson.Gson
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/gson/from"])
class GsonFromJsonEndpoints {

    @PostMapping(path = [""])
    fun post(@RequestBody json : String?) : ResponseEntity<String> {
        return try {
            val json = Gson().fromJson(json, Map::class.java)

            if (json.containsValue("teapot")) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }


            ResponseEntity.ok("OK")
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
