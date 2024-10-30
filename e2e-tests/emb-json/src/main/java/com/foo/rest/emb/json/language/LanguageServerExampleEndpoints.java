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
            ResultExtender resultExtender = new ResultExtender();

            List<RemoteRuleMatch> rules = resultExtender.getExtensionMatches("vowels");
            RemoteRuleMatch match = rules.stream()
                    .filter(rule -> "A".equals(rule.toString()))
                    .findAny()
                    .orElse(null);
            if (match != null && match.getMessage().equals("vowels")) {
                return ResponseEntity.status(200).body("vowels");
            }

            return ResponseEntity.status(204).body("Nothing found");
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
