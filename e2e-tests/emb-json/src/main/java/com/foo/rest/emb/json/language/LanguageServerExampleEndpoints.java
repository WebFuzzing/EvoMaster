package com.foo.rest.emb.json.language;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/language")
public class LanguageServerExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {

        return ResponseEntity.status(200).body("Working");
    }
}
