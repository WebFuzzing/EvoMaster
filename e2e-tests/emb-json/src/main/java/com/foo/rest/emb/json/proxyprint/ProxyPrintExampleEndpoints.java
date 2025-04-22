package com.foo.rest.emb.json.proxyprint;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(path = "/api")
public class ProxyPrintExampleEndpoints {

    @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> parseJson(@RequestBody String json) {
        PrintRequestController printRequestController = new PrintRequestController();

        try {
            Map<Long, String> budgets = printRequestController.calcBudgetForPrintRequest(json);

            if (budgets.containsKey(3L)) {
                return ResponseEntity.ok("Printing");
            }

            return ResponseEntity.status(204).body("Not printing");
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
