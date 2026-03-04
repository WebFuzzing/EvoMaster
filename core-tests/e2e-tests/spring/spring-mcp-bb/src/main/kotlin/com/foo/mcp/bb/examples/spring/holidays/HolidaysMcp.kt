package com.foo.mcp.bb.examples.spring.holidays

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException

@RestController
@CrossOrigin(origins = ["*"])
class HolidaysMcp() {

    var mcpService: McpService = McpService()
    var objectMapper: ObjectMapper = ObjectMapper()

    @PostMapping(value = ["/messages"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun messages(
        @RequestBody body: MutableMap<String, Any>
    ): ResponseEntity<String?> {
        val response: MutableMap<String?, Any?>? = mcpService.handle(body)

        if (response == null) {
            return ResponseEntity.ok().build<String?>()
        }

        try {
            val json: String = objectMapper.writeValueAsString(response)
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json)
        } catch (e: JsonProcessingException) {
            return ResponseEntity.internalServerError().body<String?>("Serialisation error: " + e.message)
        } catch (e: IOException) {
            return ResponseEntity.internalServerError().body<String?>("SSE write error: " + e.message)
        }

        return ResponseEntity.ok().build<String?>()
    }
}
