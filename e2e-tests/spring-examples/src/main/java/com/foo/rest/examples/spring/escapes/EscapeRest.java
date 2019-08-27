package com.foo.rest.examples.spring.escapes;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/escape")
public class EscapeRest {

    @RequestMapping(
            value = "/containsDollar/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EscapeResponseDto containsDollar(
            @PathVariable("s") String s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s == null || s.isEmpty()){
            dto.response = "Nope";
            dto.valid = false;
        }
        else if(s.contains("$")){
            dto.response = "This contains $";
            dto.valid = true;
        } else {
            dto.response = "Nope";
            dto.valid = false;
        }
        return dto;
    }

    @RequestMapping(
            value = "/containsQuote/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EscapeResponseDto containsQuote(
            @PathVariable("s") String s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s == null || s.isEmpty()){
            dto.response = "Nope";
            dto.valid = false;
        }
        else if(s.contains("\"")){
            dto.response = "This contains \"";
            dto.valid = true;
        } else {
            dto.response = "Nope";
            dto.valid = false;
        }
        return dto;
    }

    /*



    @RequestMapping(
            value = "/containsSlash/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EscapeResponseDto containsSlash(
            @PathVariable("s") String s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s == null || s.isEmpty()){
            dto.response = "Nope";
            dto.valid = false;
        }
        else if(s.contains("%5C")){
            dto.response = "This contains \\ aka %5C";
            dto.valid = true;
        } else {
            dto.response = "Nope";
            dto.valid = false;
        }
        return dto;
    }

    @RequestMapping(
            value = "/containsSingleQuote/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EscapeResponseDto containsSingleQuote(
            @PathVariable("s") String s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s == null || s.isEmpty()){
            dto.response = "Nope";
            dto.valid = false;
        }
        else if(s.contains("%27")){
            dto.response = "This contains \' aka %27";
            dto.valid = true;
        } else {
            dto.response = "Nope";
            dto.valid = false;
        }
        return dto;
    }
    */

}
