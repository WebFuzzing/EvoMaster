package com.foo.rest.examples.spring.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/json")
public class JsonRest {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> post(@RequestBody String json) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        Map collection = mapper.readValue(json, Map.class);
        List<Integer> z = (List<Integer>) collection.get("z");

        if (z.get(1) == 2025) {
            return ResponseEntity.ok().body("OK");
        } else {
            return ResponseEntity.badRequest().body("FAIL");
        }
    }
}
