package com.foo.rest.examples.spring.taintInvalid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Created by arcuri82 on 10-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/taintInvalid")
public class TaintInvalidRest {

    @GetMapping(path = "/{x}")
    public String getX(@PathVariable(name = "x") String x){

        List list = Arrays.asList("bar", "/", "", "", "", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..");
        if(list.contains(x)){
            return x;
        } else {
            return "foo";
        }
    }
}
