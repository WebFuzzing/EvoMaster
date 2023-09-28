package com.foo.rest.examples.spring.stringminlenght;

import com.foo.rest.examples.spring.strings.StringsResponseDto;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.ws.rs.core.MediaType;

@Validated
@RestController
@RequestMapping(path = "/api/minlength")
public class StringMinLengthRest {


    @RequestMapping(
            value = "/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public String min20(
            @PathVariable("s") @Valid @Size(min = 20) String s
    ){

        return "OK";
    }



}
