package com.foo.rest.emb.json.ocvn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

@RestController
@RequestMapping(path = "/api")
public class OcvnExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            ReleaseJsonToObject releaseJsonToObject = new ReleaseJsonToObject(json, mapper);
            Release release = null;
            release = releaseJsonToObject.toObject();
            if (release.name.equals("Darwin")) {
                return ResponseEntity.ok(release.name);
            }

            return ResponseEntity.status(204).body("Wrong release");
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
