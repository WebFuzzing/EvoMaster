package com.foo.rest.emb.json.tiltaksgjennomforing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.utils.SimpleLogger;

/**
 * This code is taken from tiltaksgjennomforing-api
 * G: https://github.com/navikt/tiltaksgjennomforing-api
 * L: MIT
 * P: tiltaksgjennomforing-api/src/main/java/no/nav/tag/tiltaksgjennomforing/varsel/notifikasjon/NotifikasjonHandler.java
 */
public class NotifikasjonHandler {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            ;

    // This doesn't belong to the original class
    private static final SimpleLogger log = new SimpleLogger();

    public <T> T readResponse(String json, Class<T> contentClass) {
        try {
            return objectMapper.readValue(json, contentClass);
        } catch (Exception exception) {
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
