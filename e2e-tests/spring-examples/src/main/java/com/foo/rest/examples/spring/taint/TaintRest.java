package com.foo.rest.examples.spring.taint;

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
@RequestMapping(path = "/api/taint")
public class TaintRest {


    @GetMapping(path = "/integer")
    public String getInteger(
            @RequestParam(name = "value", required = true)
                    String value
    ){
        int x = Integer.parseInt(value);
        return "integer " + x;
    }

    @GetMapping(path = "/date")
    public String getDate(
            @RequestParam(name = "value", required = true)
                    String value
    ){
        LocalDate x = LocalDate.parse(value);
        return "date " +x;
    }

    @GetMapping(path = "/constant")
    public String getConstant(
            @RequestParam(name = "value", required = true)
                    String value
    ){
        if(! value.equals("Hello world!!! Even if this is a long string, it will be trivial to cover with taint analysis")){
            throw new IllegalArgumentException(":-(");
        }
        return "constant OK";
    }

    @GetMapping(path = "/thirdparty")
    public String getThirdParty(
            @RequestParam(name = "value", required = true)
                    String value
    ){
        if(!TaintCheckString.check(value)){
            throw new IllegalArgumentException(":-(");
        }
        return "thirdparty OK";
    }


    @GetMapping(path = "/collection")
    public String getCollection(
            @RequestParam(name = "value", required = true)
                    String value
    ){
        List<String> list = Arrays.asList("bar12345", "foo12345");
        if(! list.contains(value)){
            throw new IllegalArgumentException(":-(");
        }

        return "collection " + value;
    }

}
