package com.foo.rest.examples.spring.filecreationissuenofault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api")
public class FileCreationIssueNoFaultRest {

    @GetMapping(value = "/{x}")
    public ResponseEntity getResource(@PathVariable("x") String x) {

        return ResponseEntity.status(HttpStatus.OK).body(x);
    }
}
