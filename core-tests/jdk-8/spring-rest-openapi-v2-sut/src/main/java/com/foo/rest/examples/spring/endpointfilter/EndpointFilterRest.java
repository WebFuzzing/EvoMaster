package com.foo.rest.examples.spring.endpointfilter;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/endpointfilter")
public class EndpointFilterRest {

    @ApiOperation(value="", tags = "Foo")
    @RequestMapping(value = "/x", method = RequestMethod.GET)
    ResponseEntity<?> getX() {
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value="", tags = "Foo")
    @RequestMapping(value = "/x", method = RequestMethod.POST)
    ResponseEntity<?> postX() {
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value="", tags = "Foo")
    @RequestMapping(value = "/x", method = RequestMethod.PUT)
    ResponseEntity<?> putX() {
        return ResponseEntity.ok().build();
    }


    @ApiOperation(value="", tags = "Bar")
    @RequestMapping(value = "/y", method = RequestMethod.GET)
    ResponseEntity<?> getY() {
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value="", tags = {"Foo", "Bar"})
    @RequestMapping(value = "/y/z", method = RequestMethod.GET)
    ResponseEntity<?> getZ() {
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value="", tags = {"Foo", "Bar"})
    @RequestMapping(value = "/y/z/k", method = RequestMethod.GET)
    ResponseEntity<?> getK() {
        return ResponseEntity.ok().build();
    }
}
