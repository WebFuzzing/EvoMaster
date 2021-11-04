package org.evomaster.client.java.controller.internal;

import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.SutHandler;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.db.SchemaExtractor;
import org.evomaster.client.java.controller.internal.db.SqlHandler;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.client.java.controller.api.ControllerConstants;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.TargetInfo;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class SutController implements SutHandler {

    private int controllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT;
    private String controllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST;

    private final SqlHandler sqlHandler = new SqlHandler();

    private Server controllerServer;

    /**
     * If using a SQL Database, gather info about its schema
     */
    private DbSchemaDto schemaDto;

    /**
     * For each action in a test, keep track of the extra heuristics, if any
     */
    private final List<ExtraHeuristicsDto> extras = new CopyOnWriteArrayList<>();

    private int actionIndex = -1;

    /**
     * Start the controller as a RESTful server.
     * Use the setters of this class to change the default
     * port and host.
     * <br>
     * This method is blocking until the server is initialized.
     *
     * @return true if there was no problem in starting the controller
     */
    public final boolean startTheControllerServer() {

        //Jersey
        ResourceConfig config = new ResourceConfig();
        config.register(JacksonFeature.class);
        config.register(new EMController(this));
        config.register(LoggingFeature.class);

        //Jetty
        controllerServer = new Server(InetSocketAddress.createUnresolved(
                getControllerHost(), getControllerPort()));

        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setShowStacks(true);
        controllerServer.setErrorHandler(errorHandler);

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        ServletContextHandler context = new ServletContextHandler(controllerServer,
                ControllerConstants.BASE_PATH + "/*");
        context.addServlet(servlet, "/*");


        try {
            controllerServer.start();
        } catch (Exception e) {
            SimpleLogger.error("Failed to start Jetty: " + e.getMessage());
            controllerServer.destroy();
        }

        //just make sure we start from a clean state
        newSearch();

        SimpleLogger.info("Started controller server on: " + controllerServer.getURI());

        return true;
    }

    public final boolean stopTheControllerServer() {
        try {
            controllerServer.stop();
            return true;
        } catch (Exception e) {
            SimpleLogger.error("Failed to stop the controller server: " + e.toString());
            return false;
        }
    }

    /**
     * @return the actual port in use (eg, if it was an ephemeral 0)
     */
    public final int getControllerServerPort() {
        return ((AbstractNetworkConnector) controllerServer.getConnectors()[0]).getLocalPort();
    }


    public final int getControllerPort() {
        return controllerPort;
    }

    public final void setControllerPort(int controllerPort) {
        this.controllerPort = controllerPort;
    }

    public final String getControllerHost() {
        return controllerHost;
    }

    public final void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }

    @Override
    public void execInsertionsIntoDatabase(List<InsertionDto> insertions) {

        Connection connection = getConnection();
        if (connection == null) {
            throw new IllegalStateException("No connection to database");
        }

        try {
            SqlScriptRunner.execInsert(connection, insertions);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getActionIndex(){
        return actionIndex;
    }

    /**
     * Calculate heuristics based on intercepted SQL commands
     *
     * @param sql command as a string
     */
    public final void handleSql(String sql) {
        Objects.requireNonNull(sql);

        sqlHandler.handle(sql);
    }

    public final void enableComputeSqlHeuristicsOrExtractExecution(boolean enableSqlHeuristics, boolean enableSqlExecution){
        sqlHandler.setCalculateHeuristics(enableSqlHeuristics);
        sqlHandler.setExtractSqlExecution(enableSqlHeuristics || enableSqlExecution);
    }


    /**
     * This is needed only during test generation (not execution),
     * and it is automatically called by the EM controller after
     * the SUT is started.
     */
    public final void initSqlHandler() {
        sqlHandler.setConnection(getConnection());
    }

    public final void resetExtraHeuristics() {
        sqlHandler.reset();
    }

    public final List<ExtraHeuristicsDto> getExtraHeuristics() {

        if (extras.size() == actionIndex) {
            extras.add(computeExtraHeuristics());
        }

        return new ArrayList<>(extras);
    }

    public final ExtraHeuristicsDto computeExtraHeuristics() {

        ExtraHeuristicsDto dto = new ExtraHeuristicsDto();

        if(sqlHandler.isCalculateHeuristics()) {
            sqlHandler.getDistances().stream()
                    .map(p ->
                            new HeuristicEntryDto(
                                    HeuristicEntryDto.Type.SQL,
                                    HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO,
                                    p.sqlCommand,
                                    p.distance
                            ))
                    .forEach(h -> dto.heuristics.add(h));
        }
        if (sqlHandler.isCalculateHeuristics() || sqlHandler.isExtractSqlExecution()){
            ExecutionDto executionDto = sqlHandler.getExecutionDto();
            dto.databaseExecutionDto = executionDto;
        }

        return dto;
    }


    /**
     * Extra information about the SQL Database Schema, if any is present.
     * Note: this is extracted by querying the database itself.
     * So the database must be up and running.
     *
     * @return a DTO with the schema information
     * @see SutController#getConnection
     */
    public final DbSchemaDto getSqlDatabaseSchema() {
        if (schemaDto != null) {
            return schemaDto;
        }

        if (getConnection() == null) {
            return null;
        }

        try {
            schemaDto = SchemaExtractor.extract(getConnection());
        } catch (Exception e) {
            SimpleLogger.error("Failed to extract the SQL Database Schema: " + e.getMessage());
            return null;
        }

        return schemaDto;
    }


    /**
     * Either there is no connection, or, if there is, then it must have P6Spy configured.
     * But this might not apply to all kind controllers
     *
     * @return false if the verification failed
     */
    public final boolean verifySqlConnection(){

        Connection connection = getConnection();
        if(connection == null
                //check does not make sense for External
                || !(this instanceof EmbeddedSutController)){
            return true;
        }

        /*
            bit hacky/brittle, but seems there is no easy way to check if a connection is
            using P6Spy.
            However, the name of driver's package would appear when doing a toString on it
         */
        String info = connection.toString();

        return info.contains("p6spy");
    }


    /**
     * Re-initialize all internal data to enable a completely new search phase
     * which should be independent from previous ones
     */
    public abstract void newSearch();

    /**
     * Re-initialize some internal data needed before running a new test
     */
    public final void newTest() {

        actionIndex = -1;
        resetExtraHeuristics();
        extras.clear();

        newTestSpecificHandler();
    }

    /**
     * As some heuristics are based on which action (eg HTTP call, or click of button)
     * in the test sequence is executed, and their order, we need to keep track of which
     * action does cover what.
     *
     * @param dto the DTO with the information about the action (eg its index in the test)
     */
    public final void newAction(ActionDto dto) {

        if (dto.index > extras.size()) {
            extras.add(computeExtraHeuristics());
        }
        this.actionIndex = dto.index;

        resetExtraHeuristics();

        newActionSpecificHandler(dto);
    }


    public abstract void newTestSpecificHandler();

    public abstract void newActionSpecificHandler(ActionDto dto);


    /**
     * Check if bytecode instrumentation is on.
     *
     * @return true if the instrumentation is on
     */
    public abstract boolean isInstrumentationActivated();

    /**
     * <p>
     * Check if the system under test (SUT) is running and fully initialized
     * </p>
     *
     * <p>
     * How to implement this method depends on the library/framework used
     * to build the application.
     * In Spring applications, this can be done with something like:
     * {@code ctx != null && ctx.isRunning()}, where {@code ctx} is a field where
     * {@code ConfigurableApplicationContext} should be stored when starting
     * the application.
     * </p>
     * @return true if the SUT is running
     */
    public abstract boolean isSutRunning();


    /**
     * <p>
     * A "," separated list of package prefixes or class names.
     * For example, "com.foo.,com.bar.Bar".
     * This is used to specify for which classes we want to measure
     * code coverage.
     * </p>
     *
     * <p>
     * Note: be careful of using something as general as "com."
     * or "org.", as most likely ALL your third-party libraries
     * would be instrumented as well, which could have a severe
     * impact on performance.
     * </p>
     *
     * @return a String representing the packages to cover
     */
    public abstract String getPackagePrefixesToCover();

    /**
     * <p>
     * If the application uses some sort of authentication, these details
     * need to be provided here.
     * Even if EvoMaster can have access to the database, it would not be able
     * to recover hashed passwords.
     * </p>
     *
     * <p>
     * To test the application, there is the need to provide auth for at least 1 user
     * (and more if they have different authorization roles).
     * When EvoMaster generates test cases, it can decide to use the credential of
     * any user provided by this method.
     * </p>
     *
     * <p>
     * What type of info to provide here depends on the auth mechanism, e.g.,
     * Basic or cookie-based (using {@link CookieLoginDto}).
     * To simplify the creation of these DTOs with auth info, you can look
     * at {@link org.evomaster.client.java.controller.AuthUtils}.
     * </p>
     *
     * <p>
     * If the credential are stored in a database, be careful on how the
     * method {@code resetStateOfSUT} is implemented.
     * If you delete all data with {@link DbCleaner}, then you will need as well to
     * recreate the auth details.
     * This can be put in a script, executed then with {@link SqlScriptRunner}.
     * </p>
     *
     * @return a list of valid authentication credentials, or {@code null} if
     *      * none is necessary
     */
    public abstract List<AuthenticationDto> getInfoForAuthentication();

    /**
     * <p>
     * If the system under test (SUT) uses a SQL database, we need to have a
     * configured connection to access it.
     * </p>
     *
     * <p>
     * This method is related to {@link SutHandler#resetStateOfSUT}.
     * When accessing a {@code Connection} object to reset the state of
     * the application, we suggest to save it to field (eg when starting the
     * application), and return such field here, e.g., {@code return connection;}.
     * This connection object will be used by EvoMaster to analyze the state of
     * the database to create better test cases.
     * </p>
     *
     * @return {@code null} if the SUT does not use any SQL database
     */
    public abstract Connection getConnection();

    /**
     * If the system under test (SUT) uses a SQL database, we need to specify
     * the driver used to connect, eg. {@code org.h2.Driver}.
     * This is needed for when we intercept SQL commands with P6Spy
     *
     * @return {@code null} if the SUT does not use any SQL database
     */
    public abstract String getDatabaseDriverName();

    public abstract List<TargetInfo> getTargetInfos(Collection<Integer> ids);

    /**
     * @return additional info for each action in the test.
     * The list is ordered based on the action index.
     */
    public abstract List<AdditionalInfo> getAdditionalInfoList();

    /**
     * <p>
     * Depending of which kind of SUT we are dealing with (eg, REST, GraphQL or SPA frontend),
     * there is different info that must be provided.
     * For example, in a RESTful API, you need to speficy where the OpenAPI/Swagger schema
     * is located.
     * </p>
     *
     * <p>
     * The interface {@link ProblemInfo} provides different implementations, like
     * {@code RestProblem}.
     * You will need to instantiate one of such classes, and return it here in this method.
     * </p>
     * @return an instance of object with all the needed data for the specific addressed problem
     */
    public abstract ProblemInfo getProblemInfo();

    /**
     * Test cases could be outputted in different language (e.g., Java and Kotlin),
     * using different testing libraries (e.g., JUnit 4 or 5).
     * Here, need to specify the default option.
     *
     * @return the format in which the test cases should be generated
     */
    public abstract SutInfoDto.OutputFormat getPreferredOutputFormat();


    public abstract UnitsInfoDto getUnitsInfoDto();

    public abstract void setKillSwitch(boolean b);


    protected UnitsInfoDto getUnitsInfoDto(UnitsInfoRecorder recorder){

        if(recorder == null){
            return null;
        }

        UnitsInfoDto dto = new UnitsInfoDto();
        dto.numberOfBranches = recorder.getNumberOfBranches();
        dto.numberOfLines = recorder.getNumberOfLines();
        dto.numberOfReplacedMethodsInSut = recorder.getNumberOfReplacedMethodsInSut();
        dto.numberOfReplacedMethodsInThirdParty = recorder.getNumberOfReplacedMethodsInThirdParty();
        dto.numberOfTrackedMethods = recorder.getNumberOfTrackedMethods();
        dto.unitNames = recorder.getUnitNames();
        dto.parsedDtos = recorder.getParsedDtos();
        dto.numberOfInstrumentedNumberComparisons = recorder.getNumberOfInstrumentedNumberComparisons();
        return dto;
    }
}
