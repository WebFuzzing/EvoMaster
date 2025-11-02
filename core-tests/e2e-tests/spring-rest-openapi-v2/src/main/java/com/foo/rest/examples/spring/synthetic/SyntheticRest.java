package com.foo.rest.examples.spring.synthetic;

import com.foo.rest.examples.spring.strings.StringsResponseDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/synthetic")
public class SyntheticRest {


    @RequestMapping(
            value = "/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String equalsFoo(
            @PathVariable("s") String s
    ){

        List<String> list = Arrays.asList(s).stream()
                .filter(it -> "foo".equals(it))
                .collect(Collectors.toList());

        if(list.isEmpty()){
            return "NOPE";
        } else {
            return "OK";
        }
    }



}
