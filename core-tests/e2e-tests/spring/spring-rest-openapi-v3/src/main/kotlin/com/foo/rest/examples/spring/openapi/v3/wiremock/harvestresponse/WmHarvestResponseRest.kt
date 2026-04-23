package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse

import com.fasterxml.jackson.databind.ObjectMapper
import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse.GLocationDto.Companion.genomicToHgvs
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/harvestresponse"])
class WmHarvestResponseRest {

    companion object{
        const val HARVEST_FOUND = "Found harvested response"
        const val HARVEST_NOT_FOUND = "Cannot find harvested response"
    }

    private val urlToGetABCMetaData = "https://list.ly/api/v4/meta?url=http%3A%2F%2Fabc.com"
    private val urlToGETGoRESTUsers = "https://gorest.co.in/public/v2/users"

    private val server = "http://grch37.rest.ensembl.org"
    private val extHgvs = "/vep/human/hgvs"
    private val extId = "/vep/human/id"

    private val extHgvs_variant = "/VARIANT?content-type=application/json&xref_refseq=1&ccds=1&canonical=1&domains=1&hgvs=1&numbers=1&protein=1"
    private val extId_params = "?content-type=application/json&xref_refseq=1&ccds=1&canonical=1&domains=1&hgvs=1&numbers=1&protein=1"

    private val token = System.getenv("GOREST_AUTH_KEY") ?: "ACCESS-TOKEN"

    private val mapper = ObjectMapper()

    val client = OkHttpClient()


    @GetMapping(path = ["/images"])
    fun getNumCountry(): ResponseEntity<String> {

        val url = URL(urlToGetABCMetaData)

        val request = Request.Builder().url(url).build()

        try {
            val data = client.newCall(request).execute()
            val body = data.body()?.string()
            val code = data.code()
            val dto = mapper.readValue(body, ListlyMetaSearchResponseDto::class.java)
            if (code != 200)
                return ResponseEntity.status(400).build()
            val num = dto?.metadata?.images?.size ?: 0
            val msg = "$code:${if (num in 1..9) "ANY FROM ONE TO NINE" else if (num >= 10) "MORE THAN 10" else "NONE"}"
            return ResponseEntity.ok(msg)
        } catch (e: Exception) {
            return ResponseEntity.status(500).build()
        }
    }

    @GetMapping(path = ["/grch37Example"])
    fun getGrch37Example(): ResponseEntity<String> {
        /*
            example from https://grch37.rest.ensembl.org/documentation/info/vep_hgvs_post

            used in EMB genome-nexus
         */

        val postBody = "{ \"hgvs_notations\" : [\"AGT:c.803T>C\", \"9:g.22125503G>C\" ] }"

        val result = makeCall(server+extHgvs, "POST", postBody)
        if (result.first != 200) {
            return ResponseEntity.status(400).build()
        }

        val list = mapper.readValue(result.second, List::class.java)
        if (list.size >= 2 && list.any { it is Map<*, *> && it.size > 10 })
            return ResponseEntity.ok(HARVEST_FOUND)
        return ResponseEntity.ok(HARVEST_NOT_FOUND)
    }

    @PostMapping(path = ["/grch37Annotation"])
    fun getGrch37Annotation(@RequestBody dtos : List<GLocationDto>): ResponseEntity<String> {

        val postBody = "{ \"hgvs_notations\" : [${dtos.joinToString(",") { "\"${genomicToHgvs(it)}\"" }}] }"


        val result = makeCall(server+extHgvs+extHgvs_variant, "POST", postBody)
        if (result.first != 200) {
            return ResponseEntity.status(400).build()
        }

        val list = mapper.readValue(result.second, List::class.java)
        if (list.isNotEmpty())
            return ResponseEntity.ok(HARVEST_FOUND)
        return ResponseEntity.ok(HARVEST_NOT_FOUND)
    }


    @GetMapping(path = ["/grch37Id"])
    fun getGrch37Id(): ResponseEntity<String> {

        val result = makeCall("$server$extId/7$extId_params", "GET", null)

        if (result.first != 200) {
            return ResponseEntity.status(400).build()
        }

        val list = mapper.readValue(result.second, List::class.java)
        if (list.isNotEmpty() && result.second.contains("refseq_transcript_ids"))
            return ResponseEntity.ok(HARVEST_FOUND)
        return ResponseEntity.ok(HARVEST_NOT_FOUND)
    }


    private fun makeCall(urlString: String, method: String, params: String?) : Pair<Int, String>{
        val connection = URL(urlString).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        //connection.setRequestProperty("Content-Length", params.toByteArray().size.toString())
        connection.useCaches = false
        connection.doInput = true
        connection.doOutput = true

        if (params != null){
            val wr = DataOutputStream(connection.outputStream)
            wr.writeBytes(params)
            wr.flush()
            wr.close()
        }


        val responseCode: Int = connection.responseCode
        val data = connection.inputStream.bufferedReader().use(BufferedReader::readText)
        return (responseCode to data)
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