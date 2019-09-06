package com.foo.rest.examples.spring.taint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
}
