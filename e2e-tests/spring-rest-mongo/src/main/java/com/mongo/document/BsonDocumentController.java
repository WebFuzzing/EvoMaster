package com.mongo.document;

import org.bson.Document;
import org.bson.json.JsonParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/bsondocument")
public class BsonDocumentController {

    @GetMapping("parse")
    public ResponseEntity<Void> parseDocument(String string) {
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


