package com.foo.rest.examples.spring.openapi.v3.wiremock.socketconnect

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/socketconnect"])
class WmSocketConnectRest {

    val client = OkHttpClient()

    @GetMapping(path = ["/string"])
    open fun getString() : ResponseEntity<String> {
        val host = "hello.there"

        val url = URL("http://$host/api/string")

        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            data.close()
            return if (data.code in 200..299){
                if (data.body?.toString() == "\"HELLO THERE!!!\""){
                    ResponseEntity.ok("Hello There")
                }else{
                    ResponseEntity.ok("OK")
                }
            } else if (data.code in 300..499){
                ResponseEntity.status(400).build()
            }else{
                ResponseEntity.status(500).build()
            }
        }catch (e: Exception){
           return ResponseEntity.status(500).build()
        }
    }


//    @GetMapping(path = ["/object"])
//    open fun getObject() : ResponseEntity<String> {
//
//        val url = URL("https://hello.there:8877/api/object")
//        val request = Request.Builder().url(url).build()
//
//        val data = client.newCall(request).execute().body?.string()
//
//        val mapper = ObjectMapper()
//        val dto = mapper.readValue(data, WmSocketConnectDto::class.java)
//
//        return if (dto.x!! > 0){
//            ResponseEntity.ok("OK")
//        } else{
//            ResponseEntity.status(500).build()
//        }
//    }
}