package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.ControllerConstants;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoDatabaseCommandDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.problem.*;
import org.evomaster.client.java.controller.mongo.MongoScriptRunner;
import org.evomaster.client.java.controller.problem.*;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.controller.problem.rpc.schema.LocalAuthSetupSchema;
import org.evomaster.client.java.instrumentation.*;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;
import org.glassfish.jersey.internal.util.Producer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Note: usually a RESTful webservice would be stateless.
 * Here, however, we have state. Reason is that we need it,
 * and the only client is the EvoMaster process. Furthermore,
 * the code of the controller should be as simple as possible,
 * as we might need to re-implement it in different languages.
 */
@Path("")
@Produces(Formats.JSON_V1)
public class EMController {

    /**
     * WARNING: make sure to call this inside a noKillSwitch
     */
    private final SutController sutController;

    /**
     * Keep track of the ID of last executed SQL command.
     * This is done to avoid repeating the same command (even if in a POST,
     * it could happen due to bugs in Jersey)
     */
    private volatile Integer lastSqlCommandId = null;

    private String baseUrlOfSUT;

    /**
     * Keep track of all host:port clients connect so far.
     * This is the mainly done for debugging, to check that we are using
     * a single TCP connection, instead of creating new ones at each request.
     * <p>
     * However, we want to check it only during testing
     */
    private static final Set<String> connectedClientsSoFar = new CopyOnWriteArraySet<>();

    private static final String htmlWarning =
            //ah! the beauty and elegance of Java when one just wants read a text resource as a string...
            new Scanner(EMController.class.getResourceAsStream("/warning.html"), "UTF-8").useDelimiter("\\A").next();

    public EMController(SutController sutController) {
        this.sutController = Objects.requireNonNull(sutController);
    }

    private boolean trackRequestSource(HttpServletRequest request) {
        String source = request.getRemoteAddr() + ":" + request.getRemotePort();
        connectedClientsSoFar.add(source);
        return true;
    }

    /**
     * Only used for debugging/testing
     *
     * @return host:port of all clients connected so far
     */
    public static Set<String> getConnectedClientsSoFar() {
        return connectedClientsSoFar;
    }

    /**
     * Only used debugging/testing
     */
    public static void resetConnectedClientsSoFar() {
        connectedClientsSoFar.clear();
    }

    private static String removePrefix(String s, String prefix) {
        if (s != null && prefix != null && s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getWarning() {
        return Response.status(400).entity(htmlWarning).build();
    }


    /**
     * This is tricky, and mainly an issue when dealing with embedded driver, where the kill-switch
     * could impact when calling Driver methods using the SUT (eg, ctx.isRunning()).
     */
    private <T> T noKillSwitch(Producer<T> lambda) {
        boolean previous = ExecutionTracer.isKillSwitch();
        ExecutionTracer.setKillSwitch(false);
        T t = lambda.call();
        ExecutionTracer.setKillSwitch(previous);
        return t;
    }

    private void noKillSwitch(Runnable lambda) {
        boolean previous = ExecutionTracer.isKillSwitch();
        ExecutionTracer.setKillSwitch(false);
        lambda.run();
        ExecutionTracer.setKillSwitch(previous);
    }

    private void noKillSwitchForceCheck(Runnable lambda) {
        /*
            Note: bit tricky for External. the calls on ExecutionTracer would have
            no impact, only those on sutController. it is needed for when driver communicates
            with SUT as part of driver operations, eg reset/seed state via an API call, which is
            done for example in Proxyprint. But for all other cases, it can be just an unnecessary
            overhead.
         */

        boolean previous = ExecutionTracer.isKillSwitch();
        sutController.setKillSwitch(false);
        lambda.run();
        sutController.setKillSwitch(previous);
    }

    @Path(ControllerConstants.INFO_SUT_PATH)
    @GET
    public Response getSutInfo(@Context HttpServletRequest httpServletRequest) {

        // this should be false when requesting SutInfo
        assert !ExecutionTracer.isExecutingAction();

        String connectionHeader = httpServletRequest.getHeader("Connection");
        if (connectionHeader == null
                || !connectionHeader.equalsIgnoreCase("keep-alive")) {
            return Response.status(400).entity(WrappedResponseDto
                    .withError("Requests should always contain a 'Connection: keep-alive'")).build();
        }

        assert trackRequestSource(httpServletRequest);

        if (!noKillSwitch(() -> sutController.verifySqlConnection())) {
            String msg = "SQL drivers are misconfigured. You must use a 'p6spy' wrapper when you " +
                    "run the SUT. For example, a database connection URL like 'jdbc:h2:mem:testdb' " +
                    "should be changed into 'jdbc:p6spy:h2:mem:testdb'. " +
                    "See documentation on how to configure P6Spy.";
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        SutInfoDto dto = new SutInfoDto();
        ProblemInfo info;

        try {
            dto.isSutRunning = noKillSwitch(() -> sutController.isSutRunning());
            dto.baseUrlOfSUT = baseUrlOfSUT;
            dto.infoForAuthentication = noKillSwitch(() -> sutController.getInfoForAuthentication());
            dto.sqlSchemaDto = noKillSwitch(() -> sutController.getSqlDatabaseSchema());
            dto.defaultOutputFormat = noKillSwitch(() -> sutController.getPreferredOutputFormat());
            info = noKillSwitch(() -> sutController.getProblemInfo());
            dto.bootTimeInfoDto = noKillSwitch(() -> sutController.getBootTimeInfoDto());
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        if (info == null) {
            String msg = "Undefined problem type in the EM Controller";
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        List<ExternalServiceDto> servicesToNotMock = info.getServicesToNotMock().stream()
                .map(s -> new ExternalServiceDto() {{
                    hostname = s.getHostname();
                    port = s.getPort();
                }})
                .collect(Collectors.toList());

        if (info instanceof RestProblem) {
            RestProblem rp = (RestProblem) info;
            dto.restProblem = new RestProblemDto();
            dto.restProblem.openApiUrl = rp.getOpenApiUrl();
            dto.restProblem.endpointsToSkip = rp.getEndpointsToSkip();
            dto.restProblem.openApiSchema = rp.getOpenApiSchema();
            dto.restProblem.servicesToNotMock = servicesToNotMock;

        } else if (info instanceof GraphQlProblem) {
            GraphQlProblem p = (GraphQlProblem) info;
            dto.graphQLProblem = new GraphQLProblemDto();
            dto.graphQLProblem.endpoint = removePrefix(p.getEndpoint(), baseUrlOfSUT);
            dto.graphQLProblem.servicesToNotMock = servicesToNotMock;

        } else if (info instanceof RPCProblem) {
            try {
                dto.rpcProblem = noKillSwitch(() -> sutController.extractRPCProblemDto(dto.isSutRunning));
                dto.rpcProblem.servicesToNotMock = servicesToNotMock;

                // send the recorded error msg to core in order to show the problem during startup
                dto.errorMsg = SimpleLogger.getRecordedErrorMsg();
            } catch (RuntimeException e) {
                String msg = e.getMessage();
                SimpleLogger.error(msg, e);
                return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
            }
        } else if(info instanceof WebProblem){
            WebProblem p = (WebProblem) info;
            dto.webProblem = new WebProblemDto();
            dto.webProblem.urlPathOfStartingPage = p.getUrlPathOfStartingPage();
            dto.webProblem.servicesToNotMock = servicesToNotMock;
        } else {
            String msg = "Unrecognized problem type: " + info.getClass().getName();
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        dto.unitsInfoDto = noKillSwitch(() -> sutController.getUnitsInfoDto());
        if (dto.unitsInfoDto == null) {
            String msg = "Failed to extract units info";
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }


        return Response.status(200).entity(WrappedResponseDto.withData(dto)).build();
    }

    @Path(ControllerConstants.CONTROLLER_INFO)
    @GET
    public Response getControllerInfoDto(@Context HttpServletRequest httpServletRequest,
                                         @QueryParam(ControllerConstants.METHOD_REPLACEMENT_CATEGORIES) String methodReplacementCategories) {

        //as the controller methods here might load classes, we need to handle this immediately
        if (methodReplacementCategories != null && !methodReplacementCategories.isEmpty()) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, methodReplacementCategories);
        }

        assert trackRequestSource(httpServletRequest);

        ControllerInfoDto dto = new ControllerInfoDto();
        dto.fullName = noKillSwitch(() -> sutController.getClass().getName());
        dto.isInstrumentationOn = noKillSwitch(() -> sutController.isInstrumentationActivated());
        dto.executableFullPath = noKillSwitch(() -> sutController.getExecutableFullPath());

        return Response.status(200).entity(WrappedResponseDto.withData(dto)).build();
    }

    @Path(ControllerConstants.NEW_SEARCH)
    @POST
    public Response newSearch(@Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        lastSqlCommandId = null;

        noKillSwitch(() -> sutController.newSearch());

        return Response.status(201).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.POST_SEARCH_ACTION)
    @POST
    @Consumes(Formats.JSON_V1)
    public Response postSearchAction(PostSearchActionDto dto, @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        try {
            noKillSwitch(() -> sutController.postSearchAction(dto));
        } catch (RuntimeException e) {
            String msg = "Failed to process post actions after search:" + e.getMessage();
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        return Response.status(201).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.RUN_SUT_PATH)
    @PUT
    @Consumes(Formats.JSON_V1)
    public Response runSut(SutRunDto dto, @Context HttpServletRequest httpServletRequest) {

        if (dto != null) {
            /*
                As this has impact on instrumentation, must be done ASAP
             */
            if (dto.methodReplacementCategories != null && !dto.methodReplacementCategories.isEmpty()) {
                System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, dto.methodReplacementCategories);
            }
        }

        assert trackRequestSource(httpServletRequest);

        /*
            If start/stop the SUT, we want to disable the killSwitch.
            The reason is it might be on from previous run, and, in such case,
            until we run a first test it would crash the SUT... eg when retrieving
            OpenAPI/GraphQL schema.

            TODO: likely all the noKillSwitch calls here are redundant
         */
        ExecutionTracer.setKillSwitch(false);
        if (sutController.isSutRunning()) {
            sutController.setKillSwitch(false);
        }

        try {
            if (dto.run == null) {
                String msg = "Invalid JSON: 'run' field is required";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            boolean sqlHeuristics = dto.calculateSqlHeuristics != null && dto.calculateSqlHeuristics;
            boolean sqlExecution = dto.extractSqlExecutionInfo != null && dto.extractSqlExecutionInfo;
            boolean advancedHeuristics = dto.advancedHeuristics != null && dto.advancedHeuristics;

            noKillSwitch(() -> sutController.enableComputeSqlHeuristicsOrExtractExecution(sqlHeuristics, sqlExecution, advancedHeuristics));

            boolean doReset = dto.resetState != null && dto.resetState;

            synchronized (this) {

                if (!dto.run) {
                    if (doReset) {
                        String msg = "Invalid JSON: cannot reset state and stop service at same time";
                        SimpleLogger.warn(msg);
                        return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
                    }

                    //if on, we want to shut down the server
                    if (noKillSwitch(() -> sutController.isSutRunning())) {
                        noKillSwitch(() -> sutController.stopSut());
                        baseUrlOfSUT = null;
                    }

                } else {
                    /*
                        If SUT is not up and running, let's start it
                     */
                    if (!noKillSwitch(() -> sutController.isSutRunning())) {
                        baseUrlOfSUT = noKillSwitch(() -> sutController.startSut());
                        if (baseUrlOfSUT == null) {
                            //there has been an internal failure in starting the SUT
                            String msg = "Internal failure: cannot start SUT based on given configuration";
                            SimpleLogger.warn(msg);
                            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
                        }
                        noKillSwitch(() -> sutController.initSqlHandler());
                        noKillSwitch(() -> sutController.registerOrExecuteInitSqlCommandsIfNeeded());
                        noKillSwitch(() -> sutController.initMongoHandler());
                    } else {
                        //TODO as starting should be blocking, need to check
                        //if initialized, and wait if not
                    }

                    /*
                        regardless of where it was running or not, need to reset state.
                        this is controlled by a boolean, although most likely we ll always
                        want to do it
                     */
                    if (dto.resetState != null && dto.resetState) {
                        try {
                            // clean db with accessed tables
                            noKillSwitchForceCheck(() -> sutController.cleanAccessedTables());

                            if (dto.resetCustomizedMethodForMockObject != null && dto.resetCustomizedMethodForMockObject){
                                noKillSwitch(()-> sutController.resetCustomizedMethodForMockObject());
                            }

                            /*
                                This should not fail... but, as it is user code, it might fail...
                                When it does, it is a major issue, as it can leave the system in
                                an inconsistent state for the following fitness evaluations.
                                So, we always force a newTest, even when reset fails.

                                TODO: a current problem is in Proxyprint, in which after REST calls
                                it seems there are locks on the DB (this might happen if a transaction
                                is started but then not committed). Ideally, in the reset of DBs we should
                                force all lock releases, and possibly point any left lock as a potential bug
                             */
                            noKillSwitchForceCheck(() -> sutController.resetStateOfSUT());
                        } finally {
                            noKillSwitch(() -> sutController.newTest());
                        }
                    }

                    /*
                        Note: here even if we start the SUT, the starting of a "New Search"
                        cannot be done here, as in this endpoint we also deal with the reset
                        of state. When we reset state for a new test run, we do not want to
                        reset all the other data regarding the whole search
                     */
                }
            }
        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.TEST_RESULTS)
    @GET
    public Response getTestResults(
            @QueryParam("ids")
            @DefaultValue("")
            String idList,
            @QueryParam("killSwitch") @DefaultValue("false")
            boolean killSwitch,
            @QueryParam("allCovered") @DefaultValue("false")
            boolean allCovered,
            @Context HttpServletRequest httpServletRequest) {

        if(allCovered && !idList.isEmpty()){
            String msg = "Cannot specify to collect all covered targets and also at same time specify some targets manually: " + idList;
            SimpleLogger.warn(msg);
            return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
        }

        // notify that actions execution is done.
        noKillSwitch(() -> sutController.setExecutingAction(false));

        assert trackRequestSource(httpServletRequest);

        try {
            TestResultsDto dto = new TestResultsDto();

            List<TargetInfo> targetInfos = null;

            if(! allCovered) {
                Set<Integer> ids;

                try {
                    ids = Arrays.stream(idList.split(","))
                            .filter(s -> !s.trim().isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet());
                } catch (NumberFormatException e) {
                    String msg = "Invalid parameter 'ids': " + e.getMessage();
                    SimpleLogger.warn(msg);
                    return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
                }

                targetInfos = noKillSwitch(() -> sutController.getTargetInfos(ids));
                if (targetInfos == null) {
                    String msg = "Failed to collect target information for " + ids.size() + " ids";
                    SimpleLogger.error(msg);
                    return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
                }
            } else{

                targetInfos = noKillSwitch(() -> sutController.getAllCoveredTargetInfos());
                if (targetInfos == null) {
                    String msg = "Failed to collect all covered target information";
                    SimpleLogger.error(msg);
                    return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
                }
            }

            targetInfos.forEach(t -> {
                TargetInfoDto info = new TargetInfoDto();
                info.id = t.mappedId;
                info.value = t.value;
                info.descriptiveId = t.descriptiveId;
                info.actionIndex = t.actionIndex;

                dto.targets.add(info);
            });

            //if we want just info on covered targets, don't add extra info...
            //however, it was problematic, as such info was used in different ways around the codebase...
            //so put back only if major performance issue, an track down each usage of each piece of info
//            if(!allCovered) {

            /*
                Note: it is important that extra is computed before AdditionalInfo,
                as heuristics on SQL might add new entries to String specializations

                FIXME actually the String specialization would work only on Embedded, and
                not on External :(
                But, as anyway we are going to refactor it in Core at a later point, no need
                to waste time for a tmp workaround
                TODO: actually ended up fixing it for External. but still need to decide if
                refactoring everything into core
             */
                dto.extraHeuristics = noKillSwitch(() -> sutController.getExtraHeuristics());

                List<AdditionalInfo> additionalInfos = noKillSwitch(() -> sutController.getAdditionalInfoList());
                if (additionalInfos != null) {
                    additionalInfos.forEach(a -> {
                        AdditionalInfoDto info = new AdditionalInfoDto();
                        info.queryParameters = new HashSet<>(a.getQueryParametersView());
                        info.headers = new HashSet<>(a.getHeadersView());
                        info.lastExecutedStatement = a.getLastExecutedStatement();
                        info.rawAccessOfHttpBodyPayload = a.isRawAccessOfHttpBodyPayload();
                        info.parsedDtoNames = new HashSet<>(a.getParsedDtoNamesView());
                        info.hostnameResolutionInfoDtos = a.getHostnameInfos().stream()
                                .map(hn -> new HostnameResolutionInfoDto(
                                        hn.getHostname(),
                                        hn.getResolvedAddress()
                                ))
                                .collect(Collectors.toList());
                        info.externalServices = a.getExternalServices().stream()
                                .map(es -> new ExternalServiceInfoDto(
                                        es.getProtocol(),
                                        es.getHostname(),
                                        es.getRemotePort()
                                ))
                                .collect(Collectors.toList());
                        info.employedDefaultWM = a.getEmployedDefaultWM().stream().map(
                                des -> new ExternalServiceInfoDto(
                                        des.getProtocol(),
                                        des.getHostname(),
                                        des.getRemotePort()
                                )
                        ).collect(Collectors.toList());
                        info.stringSpecializations = new LinkedHashMap<>();
                        for (Map.Entry<String, Set<StringSpecializationInfo>> entry :
                                a.getStringSpecializationsView().entrySet()) {

                            assert !entry.getValue().isEmpty();

                            List<StringSpecializationInfoDto> list = entry.getValue().stream()
                                    .map(it -> new StringSpecializationInfoDto(
                                            it.getStringSpecialization().toString(),
                                            it.getValue(),
                                            it.getType().toString()))
                                    .collect(Collectors.toList());

                            info.stringSpecializations.put(entry.getKey(), list);
                        }

                        dto.additionalInfoList.add(info);
                    });
                } else {
                    String msg = "Failed to collect additional info";
                    SimpleLogger.error(msg);
                    return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
                }
//            }
//        else {
//                // there is still some data that we need during minimization
//                List<AdditionalInfo> additionalInfos = noKillSwitch(() -> sutController.getAdditionalInfoList());
//                if (additionalInfos != null) {
//                    additionalInfos.forEach(a -> {
//                        AdditionalInfoDto info = new AdditionalInfoDto();
//                        info.lastExecutedStatement = a.getLastExecutedStatement();
//                        dto.additionalInfoList.add(info);
//                    });
//                }
//            }

            if (killSwitch) {
                sutController.setKillSwitch(true);
            }

            return Response.status(200).entity(WrappedResponseDto.withData(dto)).build();

        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = "Thrown exception: " + e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }
    }


    @Path(ControllerConstants.NEW_ACTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public Response newAction(ActionDto dto, @Context HttpServletRequest httpServletRequest) {
        // notify that the action is executing
        noKillSwitch(() -> sutController.setExecutingAction(true));

        // executingInitSql should be false when reaching here
        assert (!ExecutionTracer.isExecutingInitSql());
        /*
            Note: as PUT is idempotent, it can be repeated...
            so need to handle such possibility here
         */
        Integer index = dto.index;
        Integer current = sutController.getActionIndex();
        if (index == current) {
            SimpleLogger.warn("Repeated PUT on newAction for same index " + index);
        } else {

            assert trackRequestSource(httpServletRequest);

            //this MUST not be inside a noKillSwitch, as it sets to false
            sutController.newAction(dto);

            if (dto.rpcCall != null) {
                ActionResponseDto authResponseDto = null;
                if (dto.rpcCall.authSetup != null) {
                    // execute auth setup
                    authResponseDto = new ActionResponseDto();
                    try {
                        if (LocalAuthSetupSchema.isLocalAuthSetup(dto.rpcCall.authSetup)) {
                            sutController.executeHandleLocalAuthenticationSetup(dto.rpcCall.authSetup, authResponseDto);
                        } else
                            sutController.executeAction(dto.rpcCall.authSetup, authResponseDto);
                    } catch (Exception e) {
                        String msg = "Fail to execute auth setup and thrown exception: " + e.getMessage();
                        SimpleLogger.error(msg, e);
                    }
                }

                ActionResponseDto responseDto = new ActionResponseDto();
                responseDto.index = index;
                try {
                    sutController.executeAction(dto.rpcCall, responseDto);
                    if (authResponseDto != null && authResponseDto.testScript != null && !authResponseDto.testScript.isEmpty()) {
                        responseDto.testScript.addAll(0, authResponseDto.testScript);
                    }
                    return Response.status(200).entity(WrappedResponseDto.withData(responseDto)).build();
                } catch (Exception e) {
                    // TODO handle exception on responseDto later
                    String msg = "Thrown exception: " + e.getMessage();
                    SimpleLogger.error(msg, e);
                    responseDto.error500Msg = msg;
                    return Response.status(500).entity(WrappedResponseDto.withData(responseDto)).build();
                }

            }
        }

        return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.DATABASE_COMMAND)
    @Consumes(Formats.JSON_V1)
    @POST
    public Response executeDatabaseCommand(DatabaseCommandDto dto, @Context HttpServletRequest httpServletRequest) {

        Integer id = dto.idCounter;
        if (id != null) {
            if (lastSqlCommandId != null && id <= lastSqlCommandId) {
                SimpleLogger.warn("SQL command with id " + id + " has not arrived in order. Last received id : " + lastSqlCommandId);

                /*
                    if it had insertions, we silently skip doing it twice.
                    but a problem here is that we would lose any info on the auto-generated keys :(
                 */
                if (dto.insertions != null && !dto.insertions.isEmpty()) {
                    return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
                }
            }
            lastSqlCommandId = id;
        }

        assert trackRequestSource(httpServletRequest);

        try {

            /*
                ignore to collect sql created by evomaster for initialization
                note that
                    even if SqlScriptRunner.execCommand is not instrumented,
                    it can refer to a proxy, and the proxy itself is likely
                    going to be instrumented (as not in org.evomaster)
             */
            sutController.setExecutingInitSql(true);

            // collect info about tables to insert
            noKillSwitch(() -> sutController.addTableToInserted(dto.insertions.stream().map(x -> x.targetTable).collect(Collectors.toList())));

            SimpleLogger.debug("Received database command");

            Connection connection = noKillSwitch(() -> sutController.getConnectionIfExist());
            if (connection == null) {
                String msg = "No active database connection";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.command == null && (dto.insertions == null || dto.insertions.isEmpty())) {
                String msg = "No input command";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.command != null && dto.insertions != null && !dto.insertions.isEmpty()) {
                String msg = "Only 1 command can be specified";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.insertions != null) {
                if (dto.insertions.stream().anyMatch(i -> i.targetTable == null || i.targetTable.isEmpty())) {
                    String msg = "Insertion with no target table";
                    SimpleLogger.warn(msg);
                    return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
                }
            }

            QueryResult queryResult = null;
            InsertionResultsDto insertionResultsDto = null;


            try {
                if (dto.command != null) {
                    queryResult = SqlScriptRunner.execCommand(connection, dto.command);
                } else {
                    insertionResultsDto = SqlScriptRunner.execInsert(connection, dto.insertions);

                }
            } catch (Exception e) {
                String msg = "Failed to execute database command: " + e.getMessage();
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (queryResult != null) {
                return Response.status(200).entity(WrappedResponseDto.withData(queryResult.toDto())).build();
            } else if (insertionResultsDto != null) {
                return Response.status(200).entity(WrappedResponseDto.withData(insertionResultsDto)).build();
            } else {
                return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
            }

        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = "Thrown exception: " + e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        } finally {
            sutController.setExecutingInitSql(false);
        }
    }

    @Path(ControllerConstants.MONGO_INSERTION)
    @Consumes(Formats.JSON_V1)
    @POST
    public Response executeMongoInsertion(MongoDatabaseCommandDto dto, @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        try {

            sutController.setExecutingInitMongo(true);

            SimpleLogger.debug("Received mongo database command");

            Object connection = noKillSwitch(sutController::getMongoConnection);
            if (connection == null) {
                String msg = "No active database connection";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.insertions == null || dto.insertions.isEmpty()) {
                String msg = "No input command";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.insertions.stream().anyMatch(i -> i.collectionName.isEmpty() || i.databaseName.isEmpty())) {
                String msg = "Insertion with no target collection or database";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            MongoInsertionResultsDto mongoInsertionResultsDto = null;


            try {
                mongoInsertionResultsDto = MongoScriptRunner.executeInsert(connection, dto.insertions);
            } catch (Exception e) {
                String msg = "Failed to execute database command: " + e.getMessage();
                SimpleLogger.warn(msg);
                mongoInsertionResultsDto = new MongoInsertionResultsDto();
                mongoInsertionResultsDto.handleFailedInsertion(dto.insertions, e);
                return Response.status(400).entity(WrappedResponseDto.withData(mongoInsertionResultsDto)).build();
            }

            if (mongoInsertionResultsDto != null) {
                return Response.status(200).entity(WrappedResponseDto.withData(mongoInsertionResultsDto)).build();
            } else {
                return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
            }

        } catch (RuntimeException e) {
            String msg = "Thrown exception: " + e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        } finally {
            sutController.setExecutingInitMongo(false);
        }
    }
}
