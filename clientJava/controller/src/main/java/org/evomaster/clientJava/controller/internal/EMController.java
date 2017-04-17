package org.evomaster.clientJava.controller.internal;

import org.evomaster.clientJava.controller.SutController;
import org.evomaster.clientJava.controllerApi.*;
import org.evomaster.clientJava.controllerApi.dto.*;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;

import javax.ws.rs.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Note: usually a RESTful webservice would be stateless.
 * Here, however, we have state. Reason is that we need it,
 * and the only client is the EvoMaster process. Furthermore,
 * the code of the controller should be as simple as possible,
 * as we might need to re-implement it in different languages.
 */
@Path("")
public class EMController {

    private final SutController restController;
    private String baseUrlOfSUT;

    public EMController(SutController restController) {
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
        dto.infoForAuthentication = restController.getInfoForAuthentication();

        return dto;
    }

    @Path(ControllerConstants.CONTROLLER_INFO)
    @GET
    @Produces(Formats.JSON_V1)
    public ControllerInfoDto getControllerInfoDto() {

        ControllerInfoDto dto = new ControllerInfoDto();
        dto.fullName = restController.getClass().getName();
        dto.isInstrumentationOn = restController.isInstrumentationActivated();

        return dto;
    }

    @Path(ControllerConstants.NEW_SEARCH)
    @POST
    public void newSearch(){
        ExecutionTracer.reset();
        ObjectiveRecorder.reset();
    }

    @Path(ControllerConstants.RUN_SUT_PATH)
    @PUT
    @Consumes(Formats.JSON_V1)
    public void runSut(SutRunDto dto) {

        if (dto.run == null) {
            throw new WebApplicationException("Invalid JSON: 'run' field is required", 400);
        }

        boolean newlyStarted = false;

        synchronized (this) {
            if (dto.run) {
                if (!restController.isSutRunning()) {
                    baseUrlOfSUT = restController.startInstrumentedSut();
                    newlyStarted = true;
                }
            } else {
                if (restController.isSutRunning()) {
                    restController.stopSut();
                    baseUrlOfSUT = null;
                }
            }

            if (dto.resetState != null && dto.resetState) {
                if (!dto.run) {
                    throw new WebApplicationException(
                            "Invalid JSON: cannot reset state and stop service at same time");
                }

                if (!newlyStarted) { //no point resetting if fresh start
                    restController.resetStateOfSUT();
                }

                /*
                 Note: it should be fine but, if for any reason EM did not do
                 a GET on the targets, then all those newly encountered targets
                 would be lost, as EM will have no way to ask for them later, unless
                 we explicitly say to return ALL targets
                 */
                ObjectiveRecorder.clearFirstTimeEncountered();
            }

            /*
              Each time we start/stop/reset the SUT, we need to make sure
              to reset the collection of bytecode info.

              TODO: this works ONLY if SUT is running on same process
             */
            ExecutionTracer.reset();
        }
    }


    @Path(ControllerConstants.TARGETS_PATH)
    @GET
    @Produces(Formats.JSON_V1)
    public TargetsResponseDto getTargets(
            @QueryParam("ids")
            @DefaultValue("")
                    String idList) {

        //TODO: this works only if SUT runs on same process

        TargetsResponseDto dto = new TargetsResponseDto();

        Map<String, Double> objectives = ExecutionTracer.getInternalReferenceToObjectiveCoverage();

        Set<Integer> ids;

        try {
            ids = Arrays.asList(idList.split(",")).stream()
                    .filter(s -> !s.trim().isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            throw new WebApplicationException("Invalid parameter 'ids': " + e.getMessage(), e);
        }

        /*
            First, add info for all targets requested by EM
         */
        ids.stream().forEach(id -> {

            String descriptiveId = ObjectiveRecorder.getDescriptiveId(id);
            double val = objectives.getOrDefault(descriptiveId, 0d);

            TargetInfoDto info = new TargetInfoDto();
            info.id = id;
            info.value = val;
            //NO descriptiveId here

            dto.targets.add(info);
        });

        /*
         *  If new targets were found, we add them even if not requested by EM
         */
        ObjectiveRecorder.getTargetsSeenFirstTime().stream().forEach(s -> {

            double val = objectives.get(s);
            int mappedId = ObjectiveRecorder.getMappedId(s);

            TargetInfoDto info = new TargetInfoDto();
            info.id = mappedId;
            info.value = val;
            info.descriptiveId = s;

            dto.targets.add(info);
        });

        return dto;
    }
}
