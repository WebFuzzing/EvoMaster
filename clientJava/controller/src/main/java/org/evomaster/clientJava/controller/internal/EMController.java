package org.evomaster.clientJava.controller.internal;

import org.evomaster.clientJava.controller.RestController;
import org.evomaster.clientJava.controllerApi.ControllerConstants;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.SutInfoDto;
import org.evomaster.clientJava.controllerApi.SutRunDto;

import javax.ws.rs.*;
import java.util.Objects;

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
    private String baseUrlOfSUT;

    public EMController(RestController restController) {
        this.restController = Objects.requireNonNull(restController);
    }


    @Path(ControllerConstants.INFO_SUT_PATH)
    @GET
    @Produces(Formats.JSON_V1)
    public SutInfoDto getSutInfo() {

        SutInfoDto dto = new SutInfoDto();
        dto.swaggerJsonUrl = restController.getUrlOfSwaggerJSON();
        dto.isSutRunning = restController.isSutRunning();
        dto.baseUrlOfSUT = baseUrlOfSUT;

        return dto;
    }


    @Path(ControllerConstants.RUN_SUT_PATH)
    @PUT
    @Consumes(Formats.JSON_V1)
    public void runSut(SutRunDto dto) {

        if(dto.run == null){
            throw new WebApplicationException("Invalid JSON: 'run' field is required", 400);
        }

        boolean newlyStarted = false;

        if(dto.run){
            if (! restController.isSutRunning()) {
                baseUrlOfSUT = restController.startInstrumentedSut();
                newlyStarted = true;
            }
        } else {
            if (restController.isSutRunning()) {
                restController.stopSut();
                baseUrlOfSUT = null;
            }
        }

        if(dto.resetState != null && dto.resetState){
            if(! dto.run){
                throw new WebApplicationException(
                        "Invalid JSON: cannot reset state and stop service at same time");
            }

            if(! newlyStarted) { //no point resetting if fresh start
                restController.resetStateOfSUT();
            }
        }
    }
}
