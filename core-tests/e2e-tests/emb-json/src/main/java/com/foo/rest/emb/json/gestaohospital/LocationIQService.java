package com.foo.rest.emb.json.gestaohospital;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This code is taken from GestaoHospital
 * G: https://github.com/ValchanOficial/GestaoHospital
 * L: N/A
 * P: src/main/java/br/com/codenation/hospital/integration/LocationIQService.java
 */
public class LocationIQService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationIQService.class);

    public LocationIQService() {
    }

    public List<LocationIQResponse> getLocationIQResponse(String search) {
        return CallLocationIQAPI(search);
    }

    private List<LocationIQResponse> CallLocationIQAPI(String search) {
        ArrayList<LocationIQResponse> locationsResponse = new ArrayList<>();

        try {
            locationsResponse = new ObjectMapper()
                    .readValue(search, new TypeReference<ArrayList<LocationIQResponse>>() {});

        } catch (IOException e) {
            LOGGER.error("getLocationIQResponse - IOException - Error with message: {}", e.getMessage());
        }

        return locationsResponse;
    }
}
