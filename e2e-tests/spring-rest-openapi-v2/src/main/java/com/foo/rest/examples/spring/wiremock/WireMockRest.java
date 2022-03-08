package com.foo.rest.examples.spring.wiremock;

import com.foo.rest.examples.spring.strings.StringsResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/wiremock")
public class WireMockRest {

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
            value = "/external/{key}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public StringsResponseDto externalCall(
            @PathVariable("key") String key
    ) {
        StringsResponseDto stringsResponseDto = new StringsResponseDto();

        RestTemplate restTemplate = new RestTemplate();

        /**
         * The URL below is hard coded with the port of Wiremock.
         * When the key is alpha characters echo endpoint will
         * respond with the given value, otherwise Not Found!
        */
        String uri = "http://localhost:52768/api/echo/" + key;

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

        if (response.getBody().equals(key)) {
            stringsResponseDto.valid = true;
        } else {
            stringsResponseDto.valid = false;
        }
        return stringsResponseDto;
    }
}
