package com.foo.rest.examples.spring.openapi.v3.wiremock.okhttp3

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/socketconnect"])
class WmOkHttp3Rest {

    private val host = "github.com"
    private val protocol = "http"

    val client = OkHttpClient()

    @GetMapping(path = ["/string"])
    fun getString() : ResponseEntity<String> {

        val url = URL("$protocol://$host:5555/api/string")

        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body?.string()
            val code = data.code
            data.close()
            return if (code in 200..299){
                if (body == "\"HELLO THERE!!!\""){
                    ResponseEntity.ok("Hello There")
                } else{
                    ResponseEntity.ok("OK")
                }
            } else if (code in 400..499){
                ResponseEntity.status(400).build()
            }else{
                ResponseEntity.status(418).build()
            }
        }catch (e: Exception){
           return ResponseEntity.status(500).build()
        }
    }


    @GetMapping(path = ["/object"])
    fun getObject() : ResponseEntity<String> {

        val url = URL("$protocol://$host:6666/api/object")
        val request = Request.Builder().url(url).build()

        val data = client.newCall(request).execute()
        val body= data.body?.string()
        data.close()
        val mapper = ObjectMapper()
        val dto = mapper.readValue(body, WmOkHttp3Dto::class.java)

        return if (dto.x!! > 0){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }
    }

    @GetMapping(path = ["/sstring"])
    fun getSString() : ResponseEntity<String> {

        val url = URL("${protocol}s://$host:7777/api/sstring")

        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body?.string()
            val code = data.code
            data.close()
            return if (code in 200..299){
                if (body == "\"HELLO THERE!!!\""){
                    ResponseEntity.ok("Hello There")
                }else{
                    ResponseEntity.ok("OK")
                }
            } else if (code in 300..499){
                ResponseEntity.status(400).build()
            }else{
                ResponseEntity.status(418).build()
            }
        }catch (e: Exception){
            return ResponseEntity.status(500).build()
        }
    }
}
