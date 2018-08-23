package org.evomaster.clientJava.controller.internal;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.db.SqlScriptRunner;
import org.evomaster.clientJava.controllerApi.ControllerConstants;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.dto.*;
import org.evomaster.clientJava.controllerApi.dto.database.operations.DatabaseCommandDto;
import org.evomaster.clientJava.instrumentation.TargetInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
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

    private final SutController sutController;
    private String baseUrlOfSUT;

    public EMController(SutController sutController) {
        this.sutController = Objects.requireNonNull(sutController);
    }


    @Path(ControllerConstants.INFO_SUT_PATH)
    @GET
    @Produces(Formats.JSON_V1)
    public SutInfoDto getSutInfo() {

        SutInfoDto dto = new SutInfoDto();
        dto.swaggerJsonUrl = sutController.getUrlOfSwaggerJSON();
        dto.endpointsToSkip = sutController.getEndpointsToSkip();
        dto.isSutRunning = sutController.isSutRunning();
        dto.baseUrlOfSUT = baseUrlOfSUT;
        dto.infoForAuthentication = sutController.getInfoForAuthentication();
        dto.sqlSchemaDto = sutController.getSqlDatabaseSchema();

        return dto;
    }

    @Path(ControllerConstants.CONTROLLER_INFO)
    @GET
    @Produces(Formats.JSON_V1)
    public ControllerInfoDto getControllerInfoDto() {

        ControllerInfoDto dto = new ControllerInfoDto();
        dto.fullName = sutController.getClass().getName();
        dto.isInstrumentationOn = sutController.isInstrumentationActivated();

        return dto;
    }

    @Path(ControllerConstants.NEW_SEARCH)
    @POST
    public void newSearch() {
        sutController.newSearch();
    }


    @Path(ControllerConstants.RUN_SUT_PATH)
    @PUT
    @Consumes(Formats.JSON_V1)
    public void runSut(SutRunDto dto) {

        try {
            if (dto.run == null) {
                String msg = "Invalid JSON: 'run' field is required";
                SimpleLogger.warn(msg);
                throw new WebApplicationException(msg, 400);
            }

            boolean newlyStarted = false;

            synchronized (this) {
                if (dto.run) {
                    if (!sutController.isSutRunning()) {
                        baseUrlOfSUT = sutController.startSut();
                        if (baseUrlOfSUT == null) {
                            //there has been an internal failure in starting the SUT
                            throw new WebApplicationException("Internal failure: cannot start SUT based on given configuration", 500);
                        }
                        sutController.initSqlHandler();
                        sutController.newTest();
                        newlyStarted = true;
                    } else {
                        //TODO as starting should be blocking, need to check
                        //if initialized, and wait if not
                    }
                } else {
                    if (sutController.isSutRunning()) {
                        sutController.stopSut();
                        baseUrlOfSUT = null;
                    }
                }

                if (dto.resetState != null && dto.resetState) {
                    if (!dto.run) {
                        String msg = "Invalid JSON: cannot reset state and stop service at same time";
                        SimpleLogger.warn(msg);
                        throw new WebApplicationException(msg, 400);
                    }

                    if (!newlyStarted) { //no point resetting if fresh start
                        sutController.resetStateOfSUT();
                        sutController.newTest();
                    }
                }
            }
        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            SimpleLogger.error("ERROR -> " + e.getMessage());
            throw e;
        }
    }


    @Path(ControllerConstants.TARGETS_PATH)
    @GET
    @Produces(Formats.JSON_V1)
    public TargetsResponseDto getTargets(
            @QueryParam("ids")
            @DefaultValue("")
                    String idList) {

        TargetsResponseDto dto = new TargetsResponseDto();

        Set<Integer> ids;

        try {
            ids = Arrays.stream(idList.split(","))
                    .filter(s -> !s.trim().isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            String msg = "Invalid parameter 'ids': " + e.getMessage();
            SimpleLogger.warn(msg);
            throw new WebApplicationException(msg, e);
        }

        List<TargetInfo> list = sutController.getTargetInfos(ids);
        if (list == null) {
            String msg = "Failed to collect target information for " + ids.size() + " ids";
            SimpleLogger.error(msg);
            throw new WebApplicationException(msg, 500);
        }

        list.forEach(t -> {
            TargetInfoDto info = new TargetInfoDto();
            info.id = t.mappedId;
            info.value = t.value;
            info.descriptiveId = t.descriptiveId;
            info.actionIndex = t.actionIndex;

            dto.targets.add(info);
        });

        return dto;
    }


    @Path(ControllerConstants.EXTRA_HEURISTICS)
    @GET
    @Produces(Formats.JSON_V1)
    public ExtraHeuristicDto getExtra() {

        ExtraHeuristicDto dto = sutController.getExtraHeuristics();


        return dto;
    }

    @Path(ControllerConstants.EXTRA_HEURISTICS)
    @DELETE
    public void deleteExtra() {

        sutController.resetExtraHeuristics();
    }

    @Path(ControllerConstants.NEW_ACTION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @PUT
    public void newAction(@FormParam("index") int index) {

        sutController.newAction(index);
    }


    @Path(ControllerConstants.DATABASE_COMMAND)
    @Consumes(Formats.JSON_V1)
    @POST
    public void executeDatabaseCommand(DatabaseCommandDto dto) {

        Connection connection = sutController.getConnection();
        if (connection == null) {
            String msg = "No active database connection";
            SimpleLogger.warn(msg);
            throw new WebApplicationException(msg, 400);
        }

        if (dto.command == null && (dto.insertions == null || dto.insertions.isEmpty())) {
            String msg = "No input command";
            SimpleLogger.warn(msg);
            throw new WebApplicationException(msg, 400);
        }

        if (dto.command != null && dto.insertions != null && !dto.insertions.isEmpty()) {
            String msg = "Only 1 command can be specified";
            SimpleLogger.warn(msg);
            throw new WebApplicationException(msg, 400);
        }


        try {
            if (dto.command != null) {
                SqlScriptRunner.execCommand(connection, dto.command);
            } else {
                SqlScriptRunner.execInsert(connection, dto.insertions);
            }
        } catch (Exception e) {
            String msg = "Failed to execute database command: " + e.getMessage();
            SimpleLogger.warn(msg);
            throw new WebApplicationException(msg, 400);
        }
    }
}
