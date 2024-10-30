package com.foo.rest.emb.json.proxyprint;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(path = "/api")
public class ProxyPrintExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestBody String json) {
        PrintRequestController printRequestController = new PrintRequestController();

        try {
            Map<Long, String> budgets = printRequestController.calcBudgetForPrintRequest(json);

            if (!budgets.isEmpty()) {
                if (budgets.containsKey(3L)) {
                    return ResponseEntity.ok("Printing");
                }
            }

            return ResponseEntity.ok("Not printing");
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
