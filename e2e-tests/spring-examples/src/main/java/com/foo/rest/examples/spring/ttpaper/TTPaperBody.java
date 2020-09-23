package com.foo.rest.examples.spring.ttpaper;


import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@RestController
public class TTPaperBody {

    @PostMapping(path = "/api/body"
            , consumes = MediaType.APPLICATION_JSON_VALUE)
    public String post(HttpServletRequest request) throws Exception{

        String json = IOUtils.toString(request.getInputStream()
                , StandardCharsets.UTF_8);

        BodyDto dto = new Gson().fromJson(json, BodyDto.class);

        if (dto.x > 0) return "OK";
        else return null;
    }
}

class BodyDto{
    public Integer x;
}
