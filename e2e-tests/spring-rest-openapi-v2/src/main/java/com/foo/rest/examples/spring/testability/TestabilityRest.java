package com.foo.rest.examples.spring.testability;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/testability")
public class TestabilityRest {


    @GetMapping(
            path = "/{date:\\d{4}-\\d{1,2}-\\d{1,2}}/{number}/{setting}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String getSeparated(
            @PathVariable("date") String date,
            @PathVariable("number") String number,
            @PathVariable("setting") String setting
    ){

        LocalDate d = LocalDate.parse(date);
        int n = Integer.parseInt(number);
        List<String> list = Arrays.asList("Foo", "Bar");

        if(d.getYear() == 2019 && n == 42 && list.contains(setting)){
            return "OK";
        }

        return "ERROR";
    }

}
