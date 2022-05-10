package com.foo.rest.examples.spring.taintenum;

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
@RequestMapping(path = "/api/taintenum")
public class TaintEnumRest {



    @GetMapping(path = "/enum")
    public String getEnum(
            @RequestParam(name = "value", required = true)
                    String value
    ){

        TaintEnumFoo x = TaintEnumFoo.valueOf(value);

        if(x == TaintEnumFoo.HELLO){
            return "abc";
        }
        if(x == TaintEnumFoo.THERE){
            return "xyz";
        }
        return "NOPE";
    }
}
