package com.foo.rest.examples.spring.taintignorecase;

import net.thirdparty.taint.TaintCheckString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/taintIgnoreCase")
public class TaintIgnoreCaseRest {


    @GetMapping(path = "/ignorecase")
    public String getIgnoreCase(
            @RequestParam(name = "value", required = true)
                    String value
    ){
        if(value.equalsIgnoreCase("aBc123efd")
                && value.startsWith("abc")
        && value.endsWith("EFD")){
            return value;
        }

        return "";
    }


}
