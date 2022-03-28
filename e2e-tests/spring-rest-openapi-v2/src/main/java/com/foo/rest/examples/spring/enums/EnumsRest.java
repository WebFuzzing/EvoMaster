package com.foo.rest.examples.spring.enums;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(path = "/api/enums")
public class EnumsRest {

    @RequestMapping(
            value = "/{target}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public EnumsDto get(@PathVariable("target") TargetEnums target) {

        EnumsDto dto = new EnumsDto();

        switch (target){
            case A:
                dto.value = 0;
                break;
            case B:
                dto.value = 1;
                break;
        }

        return dto;
    }
}
