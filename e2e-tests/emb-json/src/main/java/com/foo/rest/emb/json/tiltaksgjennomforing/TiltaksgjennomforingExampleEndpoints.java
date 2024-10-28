package com.foo.rest.emb.json.tiltaksgjennomforing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api")
public class TiltaksgjennomforingExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {
        NotifikasjonHandler notifikasjonHandler = new NotifikasjonHandler();
        FellesResponse fellesResponse = notifikasjonHandler.readResponse(json, FellesResponse.class);

        if (fellesResponse.__typename.equals("Approved")) {
            return ResponseEntity.ok(fellesResponse.__typename);
        }

        return ResponseEntity.status(500).build();

    }
}
