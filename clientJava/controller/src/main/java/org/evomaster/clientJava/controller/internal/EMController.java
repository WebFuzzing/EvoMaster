package org.evomaster.clientJava.controller.internal;

import org.evomaster.clientJava.controller.RestController;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.SutInfoDto;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("")
public class EMController {

    private final RestController restController;

    public EMController(RestController restController) {
        this.restController = restController;
    }


    @Path("/sutInfo")
    @GET
    @Produces(Formats.JSON_V1)
    public SutInfoDto getSutInfo(){

        SutInfoDto dto = new SutInfoDto();
        dto.swaggerJsonUrl = restController.getUrlOfSwaggerJSON();

        return dto;
    }

}
