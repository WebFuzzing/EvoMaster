package com.mongo.taintrequestbody;

import org.bson.Document;
import org.bson.json.JsonParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/taintrequestbody")
public class TaintRequestBodyController {

    @PostMapping("getStringRequestBody")
    public ResponseEntity<Void> getStringRequestBody(@RequestBody String string) {
        int status;
        try {
            Document.parse(string);
            status = 200;
        } catch (JsonParseException ex) {
            status = 400;
        }
        return ResponseEntity.status(status).build();
    }

}


