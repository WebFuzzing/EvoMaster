package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

@RestController
@RequestMapping(path = ["/api/wm/harvestresponse"])
class WmHarvestResponseRest {
    private val urlToGetABCMetaData = "https://list.ly/api/v4/meta?url=http%3A%2F%2Fabc.com"
    private val urlToGETGoRESTUsers = "https://gorest.co.in/public/v2/users"

    private val server = "https://grch37.rest.ensembl.org"
    private val ext = "/vep/human/hgvs"

    private val token = System.getenv("GOREST_AUTH_KEY")?: "ACCESS-TOKEN"

    private val mapper = ObjectMapper()

    val client = OkHttpClient()


    @GetMapping(path = ["/images"])
    fun getNumCountry() : ResponseEntity<String> {

        val url = URL(urlToGetABCMetaData)

        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val code = data.code()
            val dto = mapper.readValue(body, ListlyMetaSearchResponseDto::class.java)
            if (code != 200)
                return ResponseEntity.status(400).build()
            val num = dto?.metadata?.images?.size?:0
            val msg = "$code:${if (num in 1..9) "ANY FROM ONE TO NINE" else if (num >= 10) "MORE THAN 10" else "NONE"}"
            return ResponseEntity.ok(msg)
        }catch (e: Exception){
            return ResponseEntity.status(500).build()
        }
    }

    @GetMapping(path = ["/grch37Example"])
    fun getGrch37Example() : ResponseEntity<String> {
        /*
            example from https://grch37.rest.ensembl.org/documentation/info/vep_hgvs_post

            used in EMB genome-nexus
         */

        val url = URL(server + ext)

        try {
            val connection = url.openConnection() as HttpURLConnection

            val postBody = "{ \"hgvs_notations\" : [\"AGT:c.803T>C\", \"9:g.22125503G>C\" ] }"
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Length", postBody.toByteArray().size.toString())
            connection.useCaches = false
            connection.doInput = true
            connection.doOutput = true

            val wr = DataOutputStream(connection.outputStream)
            wr.writeBytes(postBody)
            wr.flush()
            wr.close()

            val responseCode: Int = connection.responseCode
            val data = connection.inputStream.bufferedReader().use(BufferedReader::readText)

            if (responseCode != 200) {
                return ResponseEntity.status(400).build()
            }

            val list = mapper.readValue(data, List::class.java)
            if (list.size >= 2 && list.any { it is Map<*, *> && it.size > 10 })
                return ResponseEntity.ok("Found harvested response")
            return ResponseEntity.ok("Cannot find harvested response")
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }
    }

    /*
        TODO
         disable due to lack of support on readValue(String content, JavaType valueType) for Jackson
         note that need to import jackson jar for JavaType
     */
//    @GetMapping(path = ["/users"])
//    fun getNumUsers() : ResponseEntity<String> {
//
//        val url = URL(urlToGETGoRESTUsers)
//
//        val request = Request.Builder()
//            .url(url)
//            .header("Accept","application/json")
//            .header("Content-Type","application/json")
//            .header("Authorization","Bearer $token")
//            .build()
//
//        return try {
//            val data = client.newCall(request).execute()
//            val body = data.body()?.string()
//            val code = data.code()
//            val dto = mapper.readValue(body, object :TypeReference<List<UserDto>>() {})
//            if (code == 200){
//                var msg = "${if (dto.size > 10) ">10" else "<10"} users"
//                if (dto.any { d-> d.email == "foo@foo.com" && d.id == 5388})
//                    msg += " which has foo user"
//                ResponseEntity.ok(msg)
//            } else
//                ResponseEntity.status(400).build()
//        }catch (e: Exception){
//            ResponseEntity.status(500).build()
//        }
//    }
}