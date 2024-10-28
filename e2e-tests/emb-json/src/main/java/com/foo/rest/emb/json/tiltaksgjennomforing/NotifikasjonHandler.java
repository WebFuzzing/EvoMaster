package com.foo.rest.emb.json.tiltaksgjennomforing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;

public class NotifikasjonHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final ObjectMapper objectMapper;

    // This doesn't belong to the original class
    private static final SimpleLogger log = new SimpleLogger();

    public <T> T readResponse(String json, Class<T> contentClass) {
        try {
            return objectMapper.readValue(json, contentClass);
        } catch (IOException exception) {
            log.error("objectmapper feilet med lesing av data: ", exception);
        }
        return null;
    }

    public FellesResponse konverterResponse(Object data) {
        try {
            if (data != null) {
                return objectMapper.convertValue(data, FellesResponse.class);
            }
        } catch (Exception e) {
            log.error("feilet med convertering av data til FellesMutationResponse klasse: ", e);
        }
        return null;
    }

}
