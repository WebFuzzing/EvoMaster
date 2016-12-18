package org.evomaster.clientJava.controller.internal;

import org.evomaster.clientJava.controller.RestController;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.SutInfoDto;

import javax.ws.rs.*;

/**
 * Note: usually a RESTful webservice would be stateless.
 * Here, however, we have state. Reason is that we need it,
 * and the only client is the EvoMaster process. Furthermore,
 * the code of the controller should be as simple as possible,
 * as we might need to re-implement it in different languages.
 */
@Path("")
public class EMController {

    private final RestController restController;

    public EMController(RestController restController) {
        this.restController = restController;
    }


    @Path("/infoSUT")
    @GET
    @Produces(Formats.JSON_V1)
    public SutInfoDto getSutInfo(){

        SutInfoDto dto = new SutInfoDto();
        dto.swaggerJsonUrl = restController.getUrlOfSwaggerJSON();
        dto.isSutRunning = restController.isSutRunning();

        return dto;
    }


    @Path("/startSUT")
    @POST
    public void startSut(){

        if(restController.isSutRunning()){
            throw new WebApplicationException(400);
        }

        restController.startInstrumentedSut();
    }


    @Path("/stopSUT")
    @POST
    public void stopSut(){

        restController.stopSut();
    }


    @Path("/resetSUT")
    @POST
    public void resetSUT(){

        restController.resetStateOfSUT();
    }
}
