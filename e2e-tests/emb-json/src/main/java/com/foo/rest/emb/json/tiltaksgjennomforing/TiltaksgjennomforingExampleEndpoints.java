package com.foo.rest.emb.json.tiltaksgjennomforing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api")
public class TiltaksgjennomforingExampleEndpoints {

    @PostMapping(path = "/json", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> convertValue(@RequestBody String json) {
        NotifikasjonHandler notifikasjonHandler = new NotifikasjonHandler();

        OppgaveUtfoertResponse oppgaveUtfoertResponse = notifikasjonHandler.readResponse(json, OppgaveUtfoertResponse.class);

        if (oppgaveUtfoertResponse.getData() != null) {
            FellesResponse fellesResponse = notifikasjonHandler.konverterResponse(oppgaveUtfoertResponse.getData().getOppgaveUtfoertByEksternId());

            if (fellesResponse != null
                    && fellesResponse.getId().equals("2025")
                    && fellesResponse.get__typename().equals("Invoice")
            ) {
                return ResponseEntity.ok("Approved");
            }
        }

        return ResponseEntity.status(500).build();
    }
}
