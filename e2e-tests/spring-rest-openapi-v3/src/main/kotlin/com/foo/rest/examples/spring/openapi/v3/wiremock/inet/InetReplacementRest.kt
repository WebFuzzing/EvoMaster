package com.foo.rest.examples.spring.openapi.v3.wiremock.inet

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.net.URL

@RestController
@RequestMapping(path = ["/api/inet"])
class InetReplacementRest {

    @GetMapping(path = ["/exp"])
    fun InetExperiment(): ResponseEntity<String> {
        val address = InetAddress.getByName("google.com")

        val socket = Socket(address, 80)
        val output = PrintWriter(socket.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(socket.inputStream))

        output.println("Hello")
        socket.close()

        return ResponseEntity.ok("OK")
    }

    @GetMapping(path = ["/okhttp"])
    fun OkHttpExperiment(): ResponseEntity<String> {
        // TODO: Expecting this test to work in CI without any problem
        val url = URL("https://google.com:10000/")
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body?.string()
            val code = data.code
            data.close()
            return if (code in 200..299){
                if (body == "\"HELLO\""){
                    ResponseEntity.ok("Hello")
                }else{
                    ResponseEntity.ok("OK")
                }
            } else if (code in 300..499){
                ResponseEntity.status(400).build()
            }else{
                ResponseEntity.status(500).build()
            }
        } catch (e: Exception){
            return ResponseEntity.status(500).build()
        }
    }
}