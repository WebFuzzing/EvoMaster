package com.opensearch.findstring;

import com.opensearch.config.OpenSearchRepository;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/findstring")
public class OpenSearchFindStringRest {

    @Autowired
    private OpenSearchRepository openSearchRepository;

    @RequestMapping(value = "/{q}", method = RequestMethod.GET)
    public ResponseEntity<Void> findById(@PathVariable String q) throws IOException {
        Object response = openSearchRepository.search(q, Object.class);

        if (response != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}

