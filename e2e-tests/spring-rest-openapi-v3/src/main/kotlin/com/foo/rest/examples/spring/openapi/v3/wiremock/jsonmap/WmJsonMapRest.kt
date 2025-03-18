package com.foo.rest.examples.spring.openapi.v3.wiremock.jsonmap

import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.net.URL

@RestController
@RequestMapping(path = ["/api/wm/jsonmap"])
class WmJsonMapRest {

    private val gson = Gson()

    @GetMapping(path=["/gson"])
    fun getGsonObject() : ResponseEntity<String> {

        val url = URL("http://json.test:10422/api/foo")
        val connection = url.openConnection()
        connection.setRequestProperty("accept", "application/json")

        try {
            val text = BufferedReader(connection.getInputStream().reader()).readText()
            val tree = gson.fromJson(text, Map::class.java)
            if (tree.isNotEmpty()){
                var msg = "not empty map and include"
                // not solved yet
                if (tree.containsKey("foo")
                //    && tree["foo"] == "foo42"
                )
                    msg += " foo42"
                if (tree.containsKey("bar")
                    //&& tree["bar"] == "bar54"
                )
                    msg += " bar54"
                return ResponseEntity.ok(msg)
            }

            return ResponseEntity.status(404).body("empty map")
        }catch (e: JsonParseException){
            val msg = "The format of token is invalid. For example {\"key_foo\":\"value_foo\",\"key_bar\":\"value_bar\"}"
            return ResponseEntity.status(500).body(msg)
        }

    }
}
