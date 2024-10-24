package com.foo.rest.examples.spring.openapi.v3.json.gson.from

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/gson/from"])
class GsonFromJsonEndpoints {

    @PostMapping(path = ["/class"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun classOfT(@RequestBody json : String) : ResponseEntity<String> {
        // Sample JSON: {"name":"teapot"}
        return try {
            val map = Gson().fromJson(json, Map::class.java)

            if (map.containsValue("teapot")) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }


            ResponseEntity.ok("OK")
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping(path = ["/type"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun type(@RequestBody json : String) : ResponseEntity<String> {
        // Sample JSON: [{"name":"teapot"}]
        return try {
            val arrayList : ArrayList<TestDto> = Gson().fromJson(json, object : TypeToken<ArrayList<TestDto>>() {}.type)

            if (arrayList.any { it.name == "teapot" }) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }

            ResponseEntity.ok("OK")
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
