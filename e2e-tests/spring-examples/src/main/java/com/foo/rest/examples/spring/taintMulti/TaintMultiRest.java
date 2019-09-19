package com.foo.rest.examples.spring.taintMulti;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/taintMulti")
public class TaintMultiRest {


    @GetMapping(path = "/separated/{x:\\d}/{date:\\d.*}")
    public String getSeparated(
            @PathVariable("date") String date,
            @PathVariable("x") String x
    ){

        int v = Integer.parseInt(x);

        Pattern pattern = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}");
        Matcher matcher = pattern.matcher(date);
        if(!matcher.matches()){
            throw new RuntimeException();
        }

        String s = "" + v + " " + LocalDate.parse(date);

        return "separated";
    }


    @GetMapping(path = "/together/{x:\\d}-{date:\\d{4}-\\d{1,2}-\\d{1,2}}")
    public String getTogether(
            @PathVariable("date") String date,
            @PathVariable("x") String x
    ){

        return "together";
    }


}
