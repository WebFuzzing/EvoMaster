package com.foo.rest.emb.json.paypublicapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
@RestController
@RequestMapping(path = "/api")
public class PayPublicExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {
//         This use JsonNode
//        ExternalMetadata data = RequestJsonParser.parsePaymentRequest(...)

        return ResponseEntity.status(204).body("Nothing found");

    }
}
