package com.foo.rest.examples.spring.wiremock.search;

import com.foo.rest.examples.spring.strings.StringsResponseDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/wiremock")
public class SearchRest {

    @RequestMapping(
            value = "/search/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto equalsFoo(@PathVariable("s") String s) {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        stringsResponseDto.valid = "foo".equals(s);

        return stringsResponseDto;
    }
}
