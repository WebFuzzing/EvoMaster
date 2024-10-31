package com.foo.rest.emb.json.ocvn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

@RestController
@RequestMapping(path = "/api")
public class OcvnExampleEndpoints {

    @PostMapping(consumes = MediaType.APPLICATION_JSON, path = "/json", produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> parseJson(@RequestBody String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ReleaseJsonToObject releaseJsonToObject = new ReleaseJsonToObject(json, mapper);
        Release release = null;
        release = releaseJsonToObject.toObject();
        if (release.name.equals("Darwin")) {
            return ResponseEntity.ok(release.name);
        }

        return ResponseEntity.status(204).body("Wrong release");

    }
}
