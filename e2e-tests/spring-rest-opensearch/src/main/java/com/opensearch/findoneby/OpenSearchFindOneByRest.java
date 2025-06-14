package com.opensearch.findoneby;

import com.opensearch.OpenSearchRepository;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/findoneby")
public class OpenSearchFindOneByRest {

    @Autowired
    private OpenSearchRepository openSearchRepository;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Void> findById(@PathVariable String id) throws IOException {
        Object response = openSearchRepository.getById(id, Object.class);

        if (response != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}
