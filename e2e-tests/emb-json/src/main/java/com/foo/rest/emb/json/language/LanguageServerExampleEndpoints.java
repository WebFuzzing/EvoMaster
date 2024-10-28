package com.foo.rest.emb.json.language;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api")
public class LanguageServerExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson() {
        try {
            ResultExtender resultExtender = new ResultExtender("http://loca.int:9000/api/get", 2000);
            Map<String, String> params = new HashMap<>();
            List<RemoteRuleMatch> rules = resultExtender.getExtensionMatches("vowels", params);
            RemoteRuleMatch match = rules.stream()
                    .filter(rule -> "A".equals(rule.toString()))
                    .findAny()
                    .orElse(null);
            if (rules != null) {
                return ResponseEntity.status(200).body("A");
            }

            return ResponseEntity.status(204).body("Nothing found");
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
