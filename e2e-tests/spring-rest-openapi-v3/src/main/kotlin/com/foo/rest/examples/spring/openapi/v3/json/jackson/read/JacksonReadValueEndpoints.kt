package com.foo.rest.examples.spring.openapi.v3.json.jackson.read

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/jackson/read"])
class JacksonReadValueEndpoints {

    @PostMapping(path = ["/map"])
    fun postMap(@RequestBody json : String?) : ResponseEntity<String> {
        // Sample JSON: {"name":"teapot"}
        return try {
            val mapper = ObjectMapper()

            val map = mapper.readValue(json, Map::class.java)

            if (map.containsValue("teapot")) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping(path = ["/list"])
    fun postList(@RequestBody json : String?) : ResponseEntity<String> {
        // Sample JSON: ["teapot","cup"]
        return try {
            val mapper = ObjectMapper()

            val arrayList = mapper.readValue(json, ArrayList::class.java)

            if (arrayList.contains("teapot")) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping(path = ["/dto"])
    fun postListWithDTO(@RequestBody json : String?) : ResponseEntity<String> {
        // Sample JSON: [{"name":"teapot"},{"name":"cup"}]
        return try {
            val mapper = ObjectMapper()

            val arrayList = mapper.readValue(json, object : TypeReference<ArrayList<TestDto>>() {})

            if (arrayList.any { it.name == "teapot" }) {
                return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("Bingo!")
            }

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
