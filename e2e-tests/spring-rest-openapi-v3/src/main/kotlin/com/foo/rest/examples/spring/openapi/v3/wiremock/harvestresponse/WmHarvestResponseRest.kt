package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse

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

    private val mapper = ObjectMapper()

    val client = OkHttpClient()


    @GetMapping(path = ["/norway"])
    fun getString() : ResponseEntity<String> {

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
}