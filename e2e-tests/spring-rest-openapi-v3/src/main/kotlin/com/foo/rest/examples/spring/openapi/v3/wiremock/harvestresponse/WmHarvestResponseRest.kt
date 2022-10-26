package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/harvestresponse"])
class WmHarvestResponseRest {
    private val urlToGetEUCCountries = "https://api.first.org/data/v1/countries?q=norway"
    private val urlToGETGoRESTUsers = "https://gorest.co.in/public/v2/users"

    private val mapper = ObjectMapper()

    val client = OkHttpClient()


    @GetMapping(path = ["/country"])
    fun getNumCountry() : ResponseEntity<String> {

        val url = URL(urlToGetEUCCountries)

        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val code = data.code()
            val dto = mapper.readValue(body, ApiFirstCountriesResponseDto::class.java)
            if (code != 200)
                return ResponseEntity.status(400).build()
            val msg = "$code:${if ((dto?.data?.size?:0) == 1) dto.data!!.keys.first() else if ((dto?.data?.size ?: 0) > 3) "MORE THAN 3" else "NONE OR TWO"}"
            return ResponseEntity.ok(msg)
        }catch (e: Exception){
            return ResponseEntity.status(500).build()
        }
    }

    @GetMapping(path = ["/users"])
    fun getNumUsers() : ResponseEntity<String> {

        val url = URL(urlToGETGoRESTUsers)

        val request = Request.Builder()
            .url(url)
            .header("Accept","application/json")
            .header("Content-Type","application/json")
            .header("Authorization","Bearer ACCESS-TOKEN")
            .build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val code = data.code()
            val dto = mapper.readValue(body, object : TypeReference<List<UserDto>>() {})
            return if (code == 200 && dto.size == 10)
                ResponseEntity.ok("10 users as expected")
            else
                ResponseEntity.status(400).build()
        }catch (e: Exception){
            return ResponseEntity.status(500).build()
        }
    }
}