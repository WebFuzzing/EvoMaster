package com.opensearch.findoneby;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/findoneby")
public class OpenSearchFindOneByRest {

    @Autowired
    private OpenSearchClient openSearchClient;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<String> findById(@PathVariable String id) {
        try {
            GetRequest getRequest = new GetRequest.Builder()
                .index("findoneby")
                .id(id)
                .build();

            GetResponse<Object> response = openSearchClient.get(getRequest, Object.class);

            if (response.found()) {
                return ResponseEntity.ok(response.source().toString());
            } else {
                return ResponseEntity.status(404).body("Not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}
