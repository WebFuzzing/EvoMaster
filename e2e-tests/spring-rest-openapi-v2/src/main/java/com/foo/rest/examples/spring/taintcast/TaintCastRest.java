package com.foo.rest.examples.spring.taintcast;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/api/taint")
public class TaintCastRest {

    @GetMapping(path = "/cast")
    public String getTaintedArray(){
        Set<String> ids = new HashSet<>();
        ids.add("9fLSZFMq");
        ids.add("GXyM4yqq");

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        FooList list = restTemplate.postForObject("http://mock.local:13579/api/fetch", ids, FooList.class);

        List<FooDto> fooDtoList = new ArrayList<>();

        try {
            for (Object obj: ((List) list)) {
                fooDtoList.add(mapper.convertValue(obj, FooDto.class));
            }
            return "OK";
        } catch (Exception e) {
            return "NOT OK";
        }
    }
}
