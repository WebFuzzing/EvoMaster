package com.foo.rest.emb.json.signalserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api")
public class SignalServerExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {

        Optional<Account> account = AccountsManager.parseAccountJson(json, UUID.randomUUID());

        if (account.isPresent()) {
            if (account.get().getNumber().equals("5553455")) {
                return ResponseEntity.ok(account.get().getNumber());
            }
        }

        return ResponseEntity.status(204).build();

    }
}
