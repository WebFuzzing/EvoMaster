package com.foo.rest.emb.json.gestaohospital;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class GestaoHospitalExampleEndpoints {

    @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> parseJson(@RequestBody String json) {

        LocationIQService service = new LocationIQService();

        List<LocationIQResponse> responses = service.getLocationIQResponse(json);

        if (responses.get(2).getPlaceId().equals("teashop")) {
            return ResponseEntity.status(418).body("Tea");
        }

        return ResponseEntity.status(204).body("No tea for you!");

    }
}
