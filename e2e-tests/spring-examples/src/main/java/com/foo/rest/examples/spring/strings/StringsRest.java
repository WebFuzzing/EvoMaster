package com.foo.rest.examples.spring.strings;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/strings")
public class StringsRest {


    @RequestMapping(
            value = "/equalsFoo/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto equalsFoo(
            @PathVariable("s") String s
    ){
        StringsResponseDto dto = new StringsResponseDto();
        if("foo".equals(s)){
            dto.valid = true;
        } else {
            dto.valid = false;
        }

        return dto;
    }

    @RequestMapping(
            value = "/startEnds/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto startsEnd(
            @PathVariable("s") String s
    ){
        StringsResponseDto dto = new StringsResponseDto();
        if(s == null || s.isEmpty()){
            dto.valid = false;
        }
        else if(s.length() == 4 && s.startsWith("X") && s.endsWith("Y")){
            dto.valid = true;
        } else {
            dto.valid = false;
        }

        return dto;
    }

    @RequestMapping(
            value = "/contains/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto contains(
            @PathVariable("s") String s
    ){
        StringsResponseDto dto = new StringsResponseDto();
        if(s == null || s.isEmpty()){
            dto.valid = false;
        }
        else if(s.length() == 3 && "123456789".contains(s)){
            dto.valid = true;
        } else {
            dto.valid = false;
        }

        return dto;
    }


}
