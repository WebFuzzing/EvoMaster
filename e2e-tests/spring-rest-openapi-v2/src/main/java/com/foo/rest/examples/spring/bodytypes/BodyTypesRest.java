package com.foo.rest.examples.spring.bodytypes;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by arcuri82 on 07-Nov-18.
 */
@RestController
public class BodyTypesRest {

    @PostMapping(value = "/api/bodytypes/x", consumes = "application/json")
    public int jsonX( @RequestBody BodyTypesDto dto){
        return 0;
    }

    @PostMapping(value = "/api/bodytypes/x", consumes = "application/merge-patch+json")
    public int mergeX( @RequestBody BodyTypesDto dto){
        return 1;
    }

    @PostMapping(
            value = "/api/bodytypes/y",
            consumes = {"application/merge-patch+json", "application/json"})
    public int y( @RequestBody BodyTypesDto dto){
        return 2;
    }

    @PostMapping(value = "/api/bodytypes/z",
            consumes = {"application/json;charset=iso-8859-1","application/json;charset=utf-8"})
    public int charsetZ( @RequestBody BodyTypesDto dto){
        return 3;
    }


    @PostMapping(value = "/api/bodytypes/k", consumes = "text/plain")
    public int plainK( @RequestBody String text){
        return 4;
    }

    @PostMapping(value = "/api/bodytypes/k", consumes = "application/json")
    public int jsonK( @RequestBody String text){
        return 5;
    }

    //XML giving a few issues, and not so important
//    @PostMapping(value = "/api/bodytypes/x", consumes = "application/xml")
//    public int xmlX( @RequestBody BodyTypesDto dto){
//        System.out.println("X: XML");
//        return 6;
//    }


    @PostMapping(value = "/api/bodytypes/q", consumes = "application/json")
    public int jsonQ( @RequestBody BodyTypesDto dto){
        return 7;
    }

//    @PostMapping(value = "/api/bodytypes/q", consumes = "application/x-www-form-urlencoded")
//    public int formQ(Integer value){
//        return 8;
//    }


    @PostMapping(value = "/api/bodytypes/r", consumes = "application/x-www-form-urlencoded")
    public int formR(Integer value) {
        return 9;
    }

    @PostMapping(value = "/api/bodytypes/t", consumes = "application/x-www-form-urlencoded")
    public int formT(Integer a,  Integer b) {
        return 10;
    }


}
