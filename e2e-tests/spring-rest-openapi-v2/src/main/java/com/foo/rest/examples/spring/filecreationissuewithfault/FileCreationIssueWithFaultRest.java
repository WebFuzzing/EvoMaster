package com.foo.rest.examples.spring.filecreationissuewithfault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api")
public class FileCreationIssueWithFaultRest {

    @GetMapping(value = "/{x}")
    public ResponseEntity getResource(@PathVariable("x") String x) {

        return ResponseEntity.status(HttpStatus.OK).body(x);
    }

    @PostMapping(value = "/{x}")
    public ResponseEntity postResource(@PathVariable("x") String x) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(x);
    }

    @PutMapping(value = "/{x}")
    public ResponseEntity putResource(@PathVariable("x") String x) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(x);
    }

}
