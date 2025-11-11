package com.foo.rest.examples.spring.escapes;

import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/api/escape")
public class EscapeRest {

    @RequestMapping(
            value = "/containsDollar/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EscapeResponseDto containsDollar(
            @PathVariable("s") Boolean s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s){
            dto.response = "This contains $";
            dto.valid = true;
        }
        else{
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
            @PathVariable("s") Boolean s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s){
            dto.response = "This contains \"";
            dto.valid = true;
        } else {
            dto.response = "Nope";
            dto.valid = false;
        }
        return dto;
    }

    @PostMapping(value = "emptyBody", consumes = "text/plain")
    public int emptyBody( @RequestBody String text){
        if (!text.equals("\"\""))
            return 0;
        else
            return 1;
    }

    @PostMapping(value = "jsonBody",
            consumes = "application/json")
    public int jsonBody( @RequestBody EscapeResponseDto dto){
        if (dto.valid) {
            return 2;
        }
        else
        {
            return 0;
        }
    }

    @RequestMapping(
            value = "/trickyJson/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public HashMap trickyJson(
            @PathVariable("s") String s
    ){
        HashMap dto = new HashMap<String, String>();
        dto.put("Content", s);
        dto.put("Tricky-dash", "You decide");
        dto.put("Tricky.dot", "you're pushing it");

        return dto;
    }

    @RequestMapping(
            value = "/containsSlash/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EscapeResponseDto containsSlash(
            @PathVariable("s") Boolean s
    ){
        EscapeResponseDto dto = new EscapeResponseDto();
        if(s){
            dto.response = "This contains \\";
            dto.valid = true;
        } else {
            dto.response = "Nope";
            dto.valid = false;
        }
        return dto;
    }

    @RequestMapping(
            value = "/escapesJson/{s}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ArrayList<String> escapesJson(
            @PathVariable("s") Boolean s
    ){
        ArrayList dto = new ArrayList<String>();
        dto.add("$-test");
        dto.add("\\-test");
        dto.add("\"-test");
        return dto;
    }

}
