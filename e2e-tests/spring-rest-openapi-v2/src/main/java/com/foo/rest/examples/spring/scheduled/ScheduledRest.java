package com.foo.rest.examples.spring.scheduled;

import com.foo.rest.examples.spring.strings.StringsResponseDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;


@RestController
@RequestMapping(path = "/api/scheduled")
public class ScheduledRest {

    private static boolean x = false;


    public static void reset(){
        x = false;
    }

    public static boolean getValue(){
        return x ;
    }


    @Scheduled(fixedDelay = 1)
    public void updateX(){
        x = true;
    }



    @GetMapping
    public String get(){

        if(!getValue()){
            return "OK";
        }
        return "NOPE";
    }


}
