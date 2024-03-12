package com.foo.rest.examples.spring.taintnested;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/taintnested")
public class TaintNestedRest {


    @PostMapping
    public String post(
            @RequestBody TaintNestedDto dto
    ){
        if(!dto.s0.equals("This is a long string")){
            return "A";
        }
        if(!dto.s1.equals("we can handle long strings with taint analysis")){
            return "B";
        }
        if(!dto.s2.equals("but only when a tainted value is used in a replaced method call")){
            return "C";
        }
        if(!dto.s3.equals("this means that")){
            return "D";
        }
        if(!dto.s4.equals("if we return before any such call is made")){
            return "E";
        }
        if(!dto.s5.equals("then no info is collected")){
            return "F";
        }
        if(!dto.s6.equals("before reaching this method, taint analysis on same test case")){
            return "G";
        }
        if(!dto.s7.equals(" has to be solved N times")){
            return "H";
        }
        if(!dto.s8.equals("so that tainted value for s9 needs to 'survive' several iterations")){
            return "I";
        }
        if(!dto.s9.equals("before it can be finally used here")){
            return "L";
        }

        return "GOT IT!!!";
    }




}
