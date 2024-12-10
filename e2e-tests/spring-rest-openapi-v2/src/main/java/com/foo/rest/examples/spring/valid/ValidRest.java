package com.foo.rest.examples.spring.valid;



import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;


@Validated
@RestController
@RequestMapping(path = "/api/valid")
public class ValidRest {


    @RequestMapping(method = RequestMethod.POST)
    public String check(
            @RequestBody  @Valid ValidDto dto
    ){
        if(dto == null){
            return "WRONG";
        }

        return "OK";
    }
}
