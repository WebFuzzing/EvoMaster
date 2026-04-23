package com.foo.spring.rest.mongo;

import com.mongo.document.BsonDocumentApp;
import com.mongo.taintrequestbody.TaintRequestBodyApp;

public class TaintRequestBodyAppController extends MongoController {
    public TaintRequestBodyAppController() {
        super("taintrequestbody", TaintRequestBodyApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.document";
    }

}
