package com.foo.spring.rest.mongo;

import com.mongo.document.BsonDocumentApp;

public class BsonDocumentAppController extends MongoController {
    public BsonDocumentAppController() {
        super("bsondocument", BsonDocumentApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.document";
    }

}
