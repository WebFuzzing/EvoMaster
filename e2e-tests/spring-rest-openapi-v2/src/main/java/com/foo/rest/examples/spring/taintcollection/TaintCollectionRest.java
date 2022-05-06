package com.foo.rest.examples.spring.taintcollection;

import net.thirdparty.taint.TaintCheckString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/taintcollection")
public class TaintCollectionRest {


    @GetMapping(path = "/contains")
    public String contains(@RequestParam(name = "value", required = true) String value){
        Set<String> set = new HashSet<>(Arrays.asList("bar12345", "foo12345"));
        if(! set.contains(value)){
            throw new IllegalArgumentException(":-(");
        }

        return "contains OK";
    }

    @GetMapping(path = "/containsAll")
    public String containsAll(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y
            ){
        List<String> list = Arrays.asList("bar12345", "foo12345", "hello there", "tricky one");
        if(x.equals(y) || ! list.containsAll(Arrays.asList(x,y))){
            throw new IllegalArgumentException(":-(");
        }

        return "containsAll OK";
    }

    /*
        TODO
        remove
        removeAll

        Map:
        containsKey
        get
        getOrDefault
        containsValue
        remove
        replace

     */
}
