package com.foo.rest.emb.json.gestaohospital;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class GestaoHospitalExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {

        LocationIQService service = new LocationIQService();

        List<LocationIQResponse> responses = service.getLocationIQResponse(json);

        if (responses.get(3).getDisplayName().equals("teapot")) {
            return ResponseEntity.status(418).body("Found it");
        }

        return ResponseEntity.status(204).body("No tea for you!");

    }
}
