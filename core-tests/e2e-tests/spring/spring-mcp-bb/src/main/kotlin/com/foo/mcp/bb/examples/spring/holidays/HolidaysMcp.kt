package com.foo.mcp.bb.examples.spring.holidays

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@RestController
@CrossOrigin(origins = ["*"])
class HolidaysMcp(val mcpService: McpService, val objectMapper: ObjectMapper) {

//    private val mcpService: McpService? = null
//    private val objectMapper: ObjectMapper? = null

    @PostMapping(value = ["/messages"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun messages(
//        @RequestParam sessionId: String?,
        @RequestBody body: MutableMap<String, Any>
    ): ResponseEntity<String?> {
//        val emitter: SseEmitter? = sessions.get(sessionId)
//        if (emitter == null) {
//            return ResponseEntity.status(404).body<String?>("Session not found: " + sessionId)
//        }

        val response: MutableMap<String?, Any?>? = mcpService.handle(body)

        // Notifications produce no response — just acknowledge the POST
        if (response == null) {
            return ResponseEntity.ok().build<String?>()
        }

        try {
            val json: String = objectMapper.writeValueAsString(response)
            return ResponseEntity.ok(json)
//            emitter.send(
//                SseEmitter.event()
//                    .name("message")
//                    .data(json)
//            )
        } catch (e: JsonProcessingException) {
            return ResponseEntity.internalServerError().body<String?>("Serialisation error: " + e.message)
        } catch (e: IOException) {
//            sessions.remove(sessionId)
            return ResponseEntity.internalServerError().body<String?>("SSE write error: " + e.message)
        }

        return ResponseEntity.ok().build<String?>()
    }
}
