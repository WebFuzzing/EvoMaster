package com.foo.rest.emb.json.ocvn;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * This code is taken from Open Contracting Vietnam (OCVN)
 * G: https://github.com/devgateway/ocvn
 * L: MIT
 * P: src/main/java/org/devgateway/ocds/persistence/mongo/spring/json2object/ReleaseJsonToObject.java
 */
public class ReleaseJsonToObject  {
//public class ReleaseJsonToObject extends AbstractJsonToObject<Release> {
    private Release release;

    protected final ObjectMapper mapper;
    protected final String jsonObject;

    public ReleaseJsonToObject(final String jsonObject, ObjectMapper mapper) {
        this.mapper = mapper;
        this.jsonObject = jsonObject;
    }

//
//    public ReleaseJsonToObject(final String jsonObject, final Boolean mapDeserializer) {
//        super(jsonObject, mapDeserializer);
//    }
//
//    public ReleaseJsonToObject(final InputStream inputStream, final Boolean mapDeserializer) throws IOException {
//        super(inputStream, mapDeserializer);
//    }
//
//    public ReleaseJsonToObject(final File file, final Boolean mapDeserializer) throws IOException {
//        super(file, mapDeserializer);
//    }

//    @Override
    public Release toObject() throws IOException {
        if (release == null) {
            // Transform JSON String to a Release Object
            release = this.mapper.readValue(this.jsonObject, Release.class);
        }

        return release;
    }
}
