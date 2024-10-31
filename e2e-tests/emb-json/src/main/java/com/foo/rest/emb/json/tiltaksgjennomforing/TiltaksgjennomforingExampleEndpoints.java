package com.foo.rest.emb.json.tiltaksgjennomforing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api")
public class TiltaksgjennomforingExampleEndpoints {

    @PostMapping(path = "/read", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> readValue(@RequestBody String json) {
        NotifikasjonHandler notifikasjonHandler = new NotifikasjonHandler();
        FellesResponse fellesResponse = notifikasjonHandler.readResponse(json, FellesResponse.class);

        if (fellesResponse != null) {
            if (fellesResponse.id == 2025) {
                return ResponseEntity.ok("Approved");
            }
        }

        return ResponseEntity.status(500).build();

    }

    @PostMapping(path = "/convert", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> convertValue(@RequestBody String json) {
        NotifikasjonHandler notifikasjonHandler = new NotifikasjonHandler();
        FellesResponse fellesResponse = notifikasjonHandler.konverterResponse(json);

        if (fellesResponse != null) {
            if (fellesResponse.id == 2025) {
                return ResponseEntity.ok("Approved");
            }
        }

        return ResponseEntity.status(500).build();

    }
}
