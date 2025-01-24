package com.foo.rest.emb.json.genome;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.Map;

@RestController
@RequestMapping(path = "/api")
public class GenomeNexusExampleEndpoints {

    @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> parseJson(@RequestBody String json) {
        TokenMapConverter converter = new TokenMapConverter();

        Map<String, String> objects = converter.convertToMap(json);

        if (objects.containsKey("teal")) {
            return ResponseEntity.status(200).body("Teal");
        }

        return ResponseEntity.status(204).body("Working");
    }
}
