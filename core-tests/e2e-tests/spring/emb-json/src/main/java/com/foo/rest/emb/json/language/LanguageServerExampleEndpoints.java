package com.foo.rest.emb.json.language;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class LanguageServerExampleEndpoints {

    @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> parseJson(@RequestBody String json) {
        try {
            ResultExtender resultExtender = new ResultExtender();

            List<RemoteRuleMatch> rules = resultExtender.getExtensionMatches(json);

            if (rules.get(2).getMessage().equals("vowels")) {
                return ResponseEntity.ok("vowels");
            }

            return ResponseEntity.status(204).body("Nothing found");
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
