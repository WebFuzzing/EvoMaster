package org.evomaster.client.java.controller.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.evomaster.client.java.controller.CustomizationHandler;
import org.evomaster.client.java.controller.DtoUtils;
import org.evomaster.client.java.controller.SutHandler;
import org.evomaster.client.java.controller.api.ControllerConstants;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.constraint.ElementConstraintsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ExtraConstraintsDto;
import org.evomaster.client.java.controller.api.dto.MockDatabaseDto;
import org.evomaster.client.java.controller.api.dto.problem.RPCProblemDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.*;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.SqlScriptRunnerCached;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.internal.db.MongoHandler;
import org.evomaster.client.java.sql.SchemaExtractor;
import org.evomaster.client.java.sql.internal.SqlHandler;
import org.evomaster.client.java.controller.mongo.MongoScriptRunner;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;
import org.evomaster.client.java.controller.problem.rpc.CustomizedNotNullAnnotationForRPCDto;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilder;
import org.evomaster.client.java.controller.problem.rpc.RPCExceptionHandler;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.InterfaceSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.LocalAuthSetupSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.BootTimeObjectiveInfo;
import org.evomaster.client.java.instrumentation.TargetInfo;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilder.buildDbExternalServiceResponse;
import static org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilder.buildExternalServiceResponse;

/**
 * Abstract class used to connect to the EvoMaster process, and
 * that is responsible to start/stop/restart the tested application,
 * ie the system under test (SUT)
 */
public abstract class SutController implements SutHandler, CustomizationHandler {

    private int controllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT;
    private String controllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST;

    private final SqlHandler sqlHandler = new SqlHandler(new TaintHandlerExecutionTracer());

    private final MongoHandler mongoHandler = new MongoHandler();

    private Server controllerServer;

    /**
     * If using a SQL Database, gather info about its schema
     */
    private DbSchemaDto schemaDto;

    /**
     * For each action in a test, keep track of the extra heuristics, if any
     */
    private final List<ExtraHeuristicsDto> extras = new CopyOnWriteArrayList<>();

    /**
     * track all tables accessed in a test
     */
    private final List<String> accessedTables = new CopyOnWriteArrayList<>();


    /**
     * a map of table to fk target tables
     */
    private final Map<String, List<String>> fkMap = new ConcurrentHashMap<>();


    /**
     * a map of table to a set of commands which are to insert data into the db
     */
    private final Map<String, List<String>> tableInitSqlMap = new ConcurrentHashMap<>();

    /**
     * a map of interface schemas for RPC service under test
     * - key is full name of the interface
     * - value is extracted interface schema
     */
    private final Map<String, InterfaceSchema> rpcInterfaceSchema = new LinkedHashMap <>();

    /**
     * a list of jvm classes which are required to extract their schema
     */
    private final List<String> jvmClassToExtract = new CopyOnWriteArrayList<>();

    /**
     * a map of local auth setup schemas for RPC service under test
     * - key is the index of the auth info which is specified in the driver
     * - value is extracted local auth setup schema
     */
    private final Map<Integer, LocalAuthSetupSchema> localAuthSetupSchemaMap = new LinkedHashMap <>();

    /**
     * handle parsing RPCActionDto based on json string.
     * Note that it is only used for RPC
     */
    private ObjectMapper objectMapper;

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
        } catch (Exception estart) {
            String msg = "Failed to start Jetty for EM Driver: " + estart.getMessage();
            SimpleLogger.error(msg);
            try {
                controllerServer.stop();
                controllerServer.destroy();
            } catch (Exception estop) {
                SimpleLogger.error("Failed to stop Jetty: " + estop.getMessage());
            }

            throw new RuntimeException(msg,estart);
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
            SimpleLogger.error("Failed to stop the controller server: " + e);
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
    public InsertionResultsDto execInsertionsIntoDatabase(List<InsertionDto> insertions, InsertionResultsDto... previous) {

        Connection connection = getConnectionIfExist();
        if (connection == null) {
            throw new IllegalStateException("No connection to database");
        }

        try {
            return SqlScriptRunner.execInsert(connection, insertions, previous);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MongoInsertionResultsDto execInsertionsIntoMongoDatabase(List<MongoInsertionDto> insertions) {

        Object connection = getMongoConnection();
        if (connection == null) {
            throw new IllegalStateException("No connection to mongo database");
        }

        try {
            return MongoScriptRunner.executeInsert(connection, insertions);
        } catch (Exception e) {
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
    @Deprecated
    public final void handleSql(String sql) {
        Objects.requireNonNull(sql);

        sqlHandler.handle(sql);
    }

    public final void enableComputeSqlHeuristicsOrExtractExecution(
            boolean enableSqlHeuristics,
            boolean enableSqlExecution,
            boolean advancedHeuristics){
        sqlHandler.setCalculateHeuristics(enableSqlHeuristics);
        sqlHandler.setExtractSqlExecution(enableSqlHeuristics || enableSqlExecution);
        sqlHandler.setAdvancedHeuristics(advancedHeuristics);
    }


    /**
     * This is needed only during test generation (not execution),
     * and it is automatically called by the EM controller after
     * the SUT is started.
     */
    public final void initSqlHandler() {
        sqlHandler.setConnection(getConnectionIfExist());
        sqlHandler.setSchema(getSqlDatabaseSchema());
    }

    public final void initMongoHandler() {
        // This is needed because the replacement use to get this info occurs during the start of the SUT.

        List<AdditionalInfo> list = getAdditionalInfoList();
        if(!list.isEmpty()) {
            AdditionalInfo last = list.get(list.size() - 1);
            last.getMongoCollectionInfoData().forEach(mongoHandler::handle);
        }
    }


    /**
     * TODO further handle multiple connections
     * @return sql connection if there exists
     */
    public final Connection getConnectionIfExist(){
        return (getDbSpecifications() == null
                || getDbSpecifications().isEmpty())? null: getDbSpecifications().get(0).connection;
    }

    /**
     *
     * @return whether to employ smart db clean
     */
    public final boolean doEmploySmartDbClean(){
        return getDbSpecifications() != null
                && !getDbSpecifications().isEmpty() && getDbSpecifications().get(0).employSmartDbClean;
    }

    public final void resetExtraHeuristics() {
        sqlHandler.reset();
        mongoHandler.reset();
    }

    public final List<ExtraHeuristicsDto> getExtraHeuristics() {

        if (extras.size() == actionIndex) {
            extras.add(computeExtraHeuristics());
        }

        return new ArrayList<>(extras);
    }

    public final ExtraHeuristicsDto computeExtraHeuristics() {

        ExtraHeuristicsDto dto = new ExtraHeuristicsDto();

        computeSQLHeuristics(dto);
        computeMongoHeuristics(dto);

        return dto;
    }

    private void computeSQLHeuristics(ExtraHeuristicsDto dto) {
        if(sqlHandler.isCalculateHeuristics() || sqlHandler.isExtractSqlExecution()){
            /*
                TODO refactor, once we move SQL analysis into Core
             */
            List<AdditionalInfo> list = getAdditionalInfoList();
            if(!list.isEmpty()) {
                AdditionalInfo last = list.get(list.size() - 1);
                last.getSqlInfoData().stream().forEach(it -> {
//                    String sql = it.getCommand();
                    try {
                        sqlHandler.handle(new SqlExecutionLogDto(it.getCommand(), it.getExecutionTime()));
                    } catch (Exception e){
                        SimpleLogger.error("FAILED TO HANDLE SQL COMMAND: " + it.getCommand());
                        assert false; //we should try to handle all cases in our tests
                    }
                });
            }
        }

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
            // set accessed table
            if (executionDto != null){
                accessedTables.addAll(executionDto.deletedData);
                accessedTables.addAll(executionDto.insertedData.keySet());
//                accessedTables.addAll(executionDto.queriedData.keySet());
                accessedTables.addAll(executionDto.insertedData.keySet());
                accessedTables.addAll(executionDto.updatedData.keySet());
            }
        }
    }

    public final void computeMongoHeuristics(ExtraHeuristicsDto dto){
        List<AdditionalInfo> list = getAdditionalInfoList();

        if(mongoHandler.isCalculateHeuristics()){
            if(!list.isEmpty()) {
                AdditionalInfo last = list.get(list.size() - 1);
                last.getMongoInfoData().forEach(it -> {
                    try {
                        mongoHandler.handle(it);
                    } catch (Exception e){
                        SimpleLogger.error("FAILED TO HANDLE MONGO COMMAND");
                        assert false;
                    }
                });
            }

            mongoHandler.getDistances().stream()
                    .map(p ->
                            new HeuristicEntryDto(
                                    HeuristicEntryDto.Type.MONGO,
                                    HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO,
                                    p.bson.toString(),
                                    p.distance
                            ))
                    .forEach(h -> dto.heuristics.add(h));
        }

        if(mongoHandler.isExtractMongoExecution()){
            if(!list.isEmpty()) {
                AdditionalInfo last = list.get(list.size() - 1);
                last.getMongoCollectionInfoData().forEach(mongoHandler::handle);
            }
            dto.mongoExecutionDto = mongoHandler.getExecutionDto();
        }
    }


    /**
     * handle specified init sql script after SUT is started.
     */
    public final void registerOrExecuteInitSqlCommandsIfNeeded()  {
        Connection connection = getConnectionIfExist();
        if (connection == null) return;
        DbSpecification dbSpecification = getDbSpecifications().get(0);
        if (dbSpecification == null) return;
        if (!dbSpecification.employSmartDbClean) return;

        tableInitSqlMap.clear();

        try {
            setExecutingInitSql(true);
            registerInitSqlCommands(connection, dbSpecification);
        } catch (SQLException e) {
            throw new RuntimeException("Fail to register or execute the script for initializing data in SQL database, please check specified `initSqlScript` or initSqlOnResourcePath. Error Msg:", e);
        } finally {
            setExecutingInitSql(false);
        }
    }

    /**
     * perform smart db clean by cleaning the data in accessed table
     */
    public final void cleanAccessedTables(){
        if (getDbSpecifications() == null || getDbSpecifications().isEmpty()) return;
        if (getDbSpecifications().size() > 1)
            throw new RuntimeException("Error: DO NOT SUPPORT MULTIPLE SQL CONNECTION YET");

        DbSpecification emDbClean = getDbSpecifications().get(0);
        if (getConnectionIfExist() == null || !emDbClean.employSmartDbClean) return;

        try {
            setExecutingInitSql(true);

            // clean accessed tables
            Set<String> tableDataToInit = null;
            if (!accessedTables.isEmpty()){
                List<String> tablesToClean = new ArrayList<>();
                getTableToClean(accessedTables, tablesToClean);
                if (!tablesToClean.isEmpty()){
                    if (emDbClean.schemaNames != null && !emDbClean.schemaNames.isEmpty()){
                        emDbClean.schemaNames.forEach(sch-> DbCleaner.clearDatabase(getConnectionIfExist(), sch,  null, tablesToClean, emDbClean.dbType));
                    }else
                        DbCleaner.clearDatabase(getConnectionIfExist(), null,  null, tablesToClean, emDbClean.dbType);
                    tableDataToInit = tablesToClean.stream().filter(a-> tableInitSqlMap.keySet().stream().anyMatch(t-> t.equalsIgnoreCase(a))).collect(Collectors.toSet());
                }
            }
            handleInitSqlInDbClean(tableDataToInit, emDbClean);

        }catch (SQLException e) {
            throw new RuntimeException("SQL Init Execution Error: fail to execute "+e);
        }finally {
            setExecutingInitSql(false);
        }
    }

    private void handleInitSqlInDbClean(Collection<String> tableDataToInit, DbSpecification spec) throws SQLException {
        // init db script
        //boolean initAll = registerInitSqlCommands(getConnectionIfExist(), spec);
        if (tableDataToInit!= null &&!tableDataToInit.isEmpty()){
            tableDataToInit.stream().sorted((s1, s2)-> tableFkCompartor(s1, s2)).forEach(a->{
                tableInitSqlMap.keySet().stream().filter(t-> t.equalsIgnoreCase(a)).forEach(t->{
                    tableInitSqlMap.get(t).forEach(c->{
                        try {
                            SqlScriptRunner.execCommand(getConnectionIfExist(), c);
                        } catch (SQLException e) {
                            throw new RuntimeException("SQL Init Execution Error: fail to execute "+ c + " with error "+e);
                        }
                    });
                });
            });
        }
    }

    private void reAddAllInitSql() throws SQLException{
        if(tableInitSqlMap != null){
            tableInitSqlMap.keySet().stream().forEach(t->{
                tableInitSqlMap.get(t).forEach(c->{
                    try {
                        SqlScriptRunner.execCommand(getConnectionIfExist(), c);
                    } catch (SQLException e) {
                        throw new RuntimeException("SQL Init Execution Error: fail to execute "+ c + " with error "+e);
                    }
                });
            });
        }
    }

    private int tableFkCompartor(String tableA, String tableB){
        return getFkDepth(tableA, new HashSet<>()) - getFkDepth(tableB, new HashSet<>());
    }

    private int getFkDepth(String tableName, Set<String> checked){
        if(!fkMap.containsKey(tableName)) return -1;
        checked.add(tableName);
        List<String> fks = fkMap.get(tableName);
        if (fks.isEmpty()) {
            return 0;
        }
        int sum = fks.size();
        for (String fk: fks){
            if (!checked.contains(fk)){
                sum += getFkDepth(fk, checked);
            }
        }
        return sum;
    }

    /**
     * collect info about what table are manipulated by evo in order to generate data directly into it
     * @param tables a list of name of tables
     */
    public void addTableToInserted(List<String> tables){
        accessedTables.addAll(tables);
    }

    private void getTableToClean(List<String> accessedTables, List<String> tablesToClean){
        for (String t: accessedTables){
            if (!findInCollectionIgnoreCase(t, tablesToClean).isPresent()){
                if (findInMapIgnoreCase(t, fkMap).isPresent()){
                    tablesToClean.add(t);
                    List<String> fk = fkMap.entrySet().stream().filter(e->
                            findInCollectionIgnoreCase(t, e.getValue()).isPresent()
                                    && !findInCollectionIgnoreCase(e.getKey(), tablesToClean).isPresent()).map(Map.Entry::getKey).collect(Collectors.toList());
                    if (!fk.isEmpty())
                        getTableToClean(fk, tablesToClean);
                }else {
                    SimpleLogger.uniqueWarn("Cannot find the table "+t+" in ["+String.join(",", fkMap.keySet())+"]");
                }

            }
        }
    }


    private Optional<String> findInCollectionIgnoreCase(String name, Collection<String> list){
        return list.stream().filter(i-> i.equalsIgnoreCase(name)).findFirst();
    }

    private Optional<? extends Map.Entry<String, ?>> findInMapIgnoreCase(String name, Map<String, ?> list){
        return list.entrySet().stream().filter(x-> x.getKey().equalsIgnoreCase(name)).findFirst();
    }




    /**
     *
     * @param dbSpecification contains info of the db connection
     * @return whether the init script is executed
     */
    private boolean registerInitSqlCommands(Connection connection, DbSpecification dbSpecification) throws SQLException {

        if (dbSpecification.initSqlOnResourcePath == null
                && dbSpecification.initSqlScript == null) return false;

        List<String> all = new ArrayList<>();
        if (dbSpecification.initSqlOnResourcePath != null){
            all.addAll(SqlScriptRunnerCached.extractSqlScriptFromResourceFile(dbSpecification.initSqlOnResourcePath));
        }
        if (dbSpecification.initSqlScript != null){
            all.addAll(SqlScriptRunner.extractSql(dbSpecification.initSqlScript));
        }
        if (!all.isEmpty()){
            // collect insert sql commands map, key is table name, and value is a list sql insert commands
            tableInitSqlMap.putAll(SqlScriptRunner.extractSqlTableMap(all));
            /*
                comment out this for the moment
                this clean is specified by user in driver for handling the case if any table needs to be skipped
             */
//            cleanDataInDbConnection(connection, dbSpecification);
            // insert data
            SqlScriptRunner.runCommands(connection, all);
            return true;
        }
        return false;
    }

    private void cleanDataInDbConnection(Connection connection, DbSpecification dbSpecification){
        if (dbSpecification.schemaNames != null && !dbSpecification.schemaNames.isEmpty()){
            dbSpecification.schemaNames.forEach(sch-> DbCleaner.clearDatabase(connection, sch,  null, dbSpecification.dbType));
        }else
            DbCleaner.clearDatabase(connection, null, dbSpecification.dbType);
    }

    /**
     * Extra information about the SQL Database Schema, if any is present.
     * Note: this is extracted by querying the database itself.
     * So the database must be up and running.
     *
     * @return a DTO with the schema information
     * @see SutHandler#getDbSpecifications
     */
    public final DbSchemaDto getSqlDatabaseSchema() {
        if (schemaDto != null) {
            return schemaDto;
        }

        if (getDbSpecifications() == null || getDbSpecifications().isEmpty()) {
            return null;
        }

        try {
            schemaDto = SchemaExtractor.extract(getConnectionIfExist());
            Objects.requireNonNull(schemaDto);
            schemaDto.employSmartDbClean = doEmploySmartDbClean();
        } catch (Exception e) {
            SimpleLogger.error("Failed to extract the SQL Database Schema: " + e.getMessage(), e);
            return null;
        }

        if (fkMap.isEmpty()){
            schemaDto.tables.forEach(t->{
                fkMap.putIfAbsent(t.name, new ArrayList<>());
                if (t.foreignKeys!=null && !t.foreignKeys.isEmpty()){
                    t.foreignKeys.forEach(f->{
                        fkMap.get(t.name).add(f.targetTable.toUpperCase());
                    });
                }
            });
        }

        UnitsInfoDto unitsInfoDto = getUnitsInfoDto();
        List<ExtraConstraintsDto> extra = unitsInfoDto.extraDatabaseConstraintsDtos;
        if( extra != null && !extra.isEmpty()) {
            schemaDto.extraConstraintDtos = new ArrayList<>();
            schemaDto.extraConstraintDtos.addAll(extra);
        }

        return schemaDto;
    }

    /**
     *
     * @return a map from the name of interface to extracted interface
     */
    public final Map<String, InterfaceSchema> getRPCSchema(){
        return rpcInterfaceSchema;
    }

    /**
     *
     * @return a map of auth local method
     */
    public Map<Integer, LocalAuthSetupSchema> getLocalAuthSetupSchemaMap() {
        return localAuthSetupSchemaMap;
    }

    public RPCProblemDto extractRPCProblemDto(boolean isSutRunning){
        RPCProblemDto rpcProblem =  new RPCProblemDto();

        // extract RPCSchema
        extractRPCSchema();

        Map<String, InterfaceSchema> rpcSchemas = getRPCSchema();
        if (rpcSchemas == null || rpcSchemas.isEmpty()){
            throw new RuntimeException("Fail to extract RPC interface schema");
        }

        Map<Integer, LocalAuthSetupSchema> localMap = getLocalAuthSetupSchemaMap();
        if (localMap!= null && !localMap.isEmpty()){
            rpcProblem.localAuthEndpointReferences = new ArrayList<>();
            rpcProblem.localAuthEndpoints = new ArrayList<>();
            for (Map.Entry<Integer, LocalAuthSetupSchema> e : localMap.entrySet()){
                rpcProblem.localAuthEndpointReferences.add(e.getKey());
                rpcProblem.localAuthEndpoints.add(e.getValue().getDto());
            }
        }

        // handled seeded tests
        rpcProblem.seededTestDtos = handleSeededTests(isSutRunning);

        // set the schemas at the end
        rpcProblem.schemas = rpcSchemas.values().stream().map(s-> s.getDto()).collect(Collectors.toList());
        return rpcProblem;
    }

    /**
     * extract endpoints info of the RPC interface by reflection based on the specified service interface name
     */
    @Override
    public final void extractRPCSchema(){

        if (objectMapper == null)
            objectMapper = new ObjectMapper();

        if (!rpcInterfaceSchema.isEmpty())
            return;

        if (!(getProblemInfo() instanceof RPCProblem)){
            SimpleLogger.error("Problem ("+getProblemInfo().getClass().getSimpleName()+") is not RPC but request RPC schema.");
            return;
        }
        try {
            RPCEndpointsBuilder.validateCustomizedValueInRequests(getCustomizedValueInRequests());
            RPCEndpointsBuilder.validateCustomizedNotNullAnnotationForRPCDto(specifyCustomizedNotNullAnnotation());
            RPCProblem rpcp = (RPCProblem) getProblemInfo();
            for (String interfaceName: rpcp.getKeysOfMapOfInterfaceAndClient()){
                InterfaceSchema schema = RPCEndpointsBuilder.build(interfaceName, rpcp.getType(), rpcp.getClient(interfaceName),
                        rpcp.getSkipEndpointsByName()!=null? rpcp.getSkipEndpointsByName().get(interfaceName):null,
                        rpcp.getSkipEndpointsByAnnotation()!=null?rpcp.getSkipEndpointsByAnnotation().get(interfaceName):null,
                        rpcp.getInvolveEndpointsByName()!=null? rpcp.getInvolveEndpointsByName().get(interfaceName):null,
                        rpcp.getInvolveEndpointsByAnnotation()!=null? rpcp.getInvolveEndpointsByAnnotation().get(interfaceName):null,
                        getInfoForAuthentication(),
                        getCustomizedValueInRequests(),
                        specifyCustomizedNotNullAnnotation());
                rpcInterfaceSchema.put(interfaceName, schema);
            }
            localAuthSetupSchemaMap.clear();
            Map<Integer, LocalAuthSetupSchema> local = RPCEndpointsBuilder.buildLocalAuthSetup(getInfoForAuthentication());
            if (local!=null && !local.isEmpty())
                localAuthSetupSchemaMap.putAll(local);
        }catch (Exception e){
            throw new RuntimeException("Failed to extract the RPC Schema: " + e.getMessage());
        }
    }

    /**
     * parse seeded tests for RPC
     * @return seeded tests with a map,
     *      key is a name of the seeded test case,
     *      value is a list of RCPActionDto for the test case
     */
    public Map<String, List<RPCActionDto>> handleSeededTests(boolean isSUTRunning){
        List<SeededRPCTestDto> seedRPCTests;

        try {
            // customized implementation might bring some exception
            seedRPCTests = seedRPCTests();
        }catch (Exception e){
            throw new RuntimeException("cannot process the implemented method 'seedRPCTests' due to ", e);
        }

        if (seedRPCTests == null || seedRPCTests.isEmpty()) return null;

        if (rpcInterfaceSchema.isEmpty())
            throw new IllegalStateException("empty RPC interface: The RPC interface schemas are not extracted yet");

        ProblemInfo rpcp = getProblemInfo();
        if (!(rpcp instanceof  RPCProblem))
            throw new IllegalStateException("EM driver RPC: the specified problem is not RPC");
        RPCType rpcType = ((RPCProblem) rpcp).getType();

        return RPCEndpointsBuilder.buildSeededTest(rpcInterfaceSchema, seedRPCTests, rpcType);

//        try{
//            if (isSUTRunning){
//                if (jvmClassToExtract.isEmpty()){
//                /*
//                    distinct might be a bit expensive, however, the specified responses are probably limited
//                 */
//                    Set<String> dtoNames = seedRPCTests.stream()
//                            .flatMap(s-> s.rpcFunctions == null? Stream.empty() : s.rpcFunctions.stream()
//                                    .flatMap(f-> f.mockRPCExternalServiceDtos == null ? Stream.empty() : f.mockRPCExternalServiceDtos.stream()
//                                            .flatMap(e-> e.responseTypes == null ? Stream.empty(): e.responseTypes.stream()))).collect(Collectors.toSet());
//                    if (dtoNames != null && !dtoNames.isEmpty())
//                        jvmClassToExtract.addAll(dtoNames);
//                }
//
//                if (!jvmClassToExtract.isEmpty())
//                    getJvmDtoSchema(jvmClassToExtract);
//            }
//        }catch (Exception e){
//            SimpleLogger.recordErrorMessage("Fail to extract JVM Class due to "+ e.getMessage());
//        }
//
//        return results;
    }


    /**
     * Either there is no connection, or, if there is, then it must have P6Spy configured.
     * But this might not apply to all kind controllers
     *
     * @return false if the verification failed
     */
    @Deprecated
    public final boolean verifySqlConnection(){

        return true;

//        Connection connection = getConnection();
//        if(connection == null
//                //check does not make sense for External
//                || !(this instanceof EmbeddedSutController)){
//            return true;
//        }
//
//        /*
//            bit hacky/brittle, but seems there is no easy way to check if a connection is
//            using P6Spy.
//            However, the name of driver's package would appear when doing a toString on it
//         */
//        String info = connection.toString();
//
//        return info.contains("p6spy");
    }


    /**
     * Re-initialize all internal data to enable a completely new search phase
     * which should be independent from previous ones
     */
    public abstract void newSearch();

    /**
     * handling post actions after the search
     * @param dto contains required info for the post handling
     */
    public void postSearchAction(PostSearchActionDto dto){
        try{
            if (dto != null && dto.rpcTests != null && !dto.rpcTests.isEmpty()){
                dto.rpcTests.forEach(s->
                        customizeRPCTestOutput(s.externalServiceDtos, s.sqlInsertions, s.actions)
                );
            }
        }catch (Exception e){
            throw new RuntimeException("fail to customize RPC Test outputs:", e);
        }
    }

    /**
     * Re-initialize some internal data needed before running a new test
     */
    public final void newTest() {

        actionIndex = -1;
        resetExtraHeuristics();
        extras.clear();

        //clean all accessed table in a test
        accessedTables.clear();

        newTestSpecificHandler();

        // set executingAction state false for newTest
        setExecutingAction(false);
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

    public final void executeHandleLocalAuthenticationSetup(RPCActionDto dto, ActionResponseDto responseDto){

        LocalAuthSetupSchema endpointSchema = new LocalAuthSetupSchema();
        endpointSchema.setValue(dto);
        handleLocalAuthenticationSetup(endpointSchema.getAuthenticationInfo());

        if (dto.responseVariable != null && dto.doGenerateTestScript && DtoUtils.isJavaOrKotlin(dto.outputFormat)){
            responseDto.testScript = endpointSchema.newInvocationWithJavaOrKotlin(dto.responseVariable, dto.controllerVariable,dto.clientVariable, dto.outputFormat);
        }
    }

    /**
     * execute a RPC request based on the specified dto
     * @param dto is the action DTO to be executed
     */
    public final void executeAction(RPCActionDto dto, ActionResponseDto responseDto) {
        EndpointSchema endpointSchema = getEndpointSchema(dto);
        if (dto.responseVariable != null && dto.doGenerateTestScript){
            if (dto.outputFormat == null)
                throw new IllegalArgumentException("When doGenerateTestScript is specified as True, outputFormat cannot be null");

            if (DtoUtils.isJavaOrKotlin(dto.outputFormat)){
                try{
                    responseDto.testScript = endpointSchema.newInvocationWithJavaOrKotlin(dto.responseVariable, dto.controllerVariable,dto.clientVariable, dto.outputFormat);
                }catch (Exception e){
                    // for tests
                    assert(false);
                    SimpleLogger.warn("Fail to generate test script "+e.getMessage());
                }
                if (responseDto.testScript ==null)
                    SimpleLogger.warn("Null test script for action "+dto.actionName);
            }

        }

        Object response;
        try {
            if (dto.mockRPCExternalServiceDtos != null && !dto.mockRPCExternalServiceDtos.isEmpty()){
                Boolean ok = handleCustomizedMethod(()->customizeMockingRPCExternalService(dto.mockRPCExternalServiceDtos, true));
                if (ok == null || !ok)
                    SimpleLogger.warn("Warning: Fail to start mocked instances of RPC-based external services with the customized method");
            }
            if (dto.mockDatabaseDtos != null && !dto.mockDatabaseDtos.isEmpty()){
                Boolean ok = handleCustomizedMethod(()-> customizeMockingDatabase(dto.mockDatabaseDtos, true));
                if (ok == null || !ok)
                    SimpleLogger.warn("Warning: Fail to start mocked instances of databases with the customized method");
            }
            response = executeRPCEndpoint(dto, false);
            expandMockObjectIfNeeded(dto, responseDto);
        } catch (Exception e) {
            throw new RuntimeException("ERROR: target exception should be caught, but "+ e.getMessage());
        } finally {
            if (dto.mockRPCExternalServiceDtos != null && !dto.mockRPCExternalServiceDtos.isEmpty())
                handleCustomizedMethod(()-> customizeMockingRPCExternalService(dto.mockRPCExternalServiceDtos, false)); // disable mocked responses
            if (dto.mockDatabaseDtos != null && !dto.mockDatabaseDtos.isEmpty())
                handleCustomizedMethod(()-> customizeMockingDatabase(dto.mockDatabaseDtos, false)); // disable mock objects for database
        }

        //handle exception
        if (response instanceof Exception){
            try{
                Map<Class, Integer> levelsMap = null;
                try{
                    levelsMap = getExceptionImportanceLevels();
                }catch (Throwable e){
                    SimpleLogger.error("ERROR: fail to get specified importance levels for exceptions "+ e.getMessage());
                }
                RPCExceptionHandler.handle(response, responseDto, endpointSchema, getRPCType(dto), levelsMap);
                return;
            } catch (Exception e){
                SimpleLogger.error("ERROR: fail to handle exception instance to dto "+ e.getMessage());
                //throw new RuntimeException("ERROR: fail to handle exception instance to dto "+ e.getMessage());
            }
        }

        if (endpointSchema.getResponse() != null){
            // successful execution
            NamedTypedValue resSchema = endpointSchema.getResponse().copyStructureWithProperties();
            if (response != null){
                try{
                    resSchema.setValueBasedOnInstance(response);
                    responseDto.rpcResponse = resSchema.getDto();
                    if (dto.doGenerateAssertions && dto.responseVariable != null && DtoUtils.isJavaOrKotlin(dto.outputFormat)){
                        try{
                            responseDto.assertionScript = resSchema.newAssertionWithJavaOrKotlin(dto.responseVariable, dto.maxAssertionForDataInCollection, DtoUtils.isJava(dto.outputFormat));
                        }catch (Exception e){
                            // for tests
                            assert(false);
                            SimpleLogger.error("ERROR: fail to handle assertion generations with the given response "+ e.getMessage());
                        }
                    }
                    /*
                        ActionResponseDto.jsonResponse could be used to generate assertions in core side
                        however, as we do not support the test generate in core side yet and not all DTO can be converted into json,
                        we comment out this code
                     */
//                    else{
//                        try {
//                            responseDto.jsonResponse = objectMapper.writeValueAsString(response);
//                        }catch (JsonProcessingException e){
//                            // cannot convert to json
//                        }
//                    }

                } catch (Exception e){
                    SimpleLogger.error("ERROR: fail to set successful response instance value to dto "+ e.getMessage());
                    //throw new RuntimeException("ERROR: fail to set successful response instance value to dto "+ e.getMessage());
                }

                try {
                    responseDto.customizedCallResultCode = categorizeBasedOnResponse(response);
                } catch (Exception e){
                    SimpleLogger.error("ERROR: fail to categorize result with implemented categorizeBasedOnResponse "+ e.getMessage());
                    //throw new RuntimeException("ERROR: fail to categorize result with implemented categorizeBasedOnResponse "+ e.getMessage());
                }
            } else {
                if (dto.doGenerateAssertions && dto.responseVariable != null && DtoUtils.isJavaOrKotlin(dto.outputFormat))
                    responseDto.assertionScript = resSchema.newAssertionWithJavaOrKotlin(dto.responseVariable, dto.maxAssertionForDataInCollection, DtoUtils.isJava(dto.outputFormat));
            }
        }
    }


    /**
     * avoid any exception introduced by customized method
     */
    private<T> T handleCustomizedMethod(Supplier<T> call){
        try{
            return call.get();
        }catch (Throwable e){
            SimpleLogger.error("ERROR: Fail to process mocking with customized method:", e);
        }
        return null;
    }

    private Object executeRPCEndpoint(RPCActionDto dto, boolean throwTargetException) throws Exception {
        Object client = ((RPCProblem)getProblemInfo()).getClient(dto.interfaceId);
        EndpointSchema endpointSchema = getEndpointSchema(dto);
        return executeRPCEndpointCatchTargetException(client, endpointSchema, throwTargetException);
    }

    private Object executeRPCEndpointCatchTargetException(Object client, EndpointSchema endpoint, boolean throwTargetException) throws Exception {

        Object res;
        try {
            res = executeRPCEndpoint(client, endpoint);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("EM RPC REQUEST EXECUTION ERROR: fail to process a RPC request with "+ e.getMessage());
        } catch (InvocationTargetException e) {
            if (throwTargetException)
                throw (Exception) e.getTargetException();
            else
                res = e.getTargetException();
        } catch (Exception e){
            SimpleLogger.error("ERROR: other exception exists "+ e.getMessage());
            if (throwTargetException) throw e;
            else res = e;
        }
        return res;
    }

    @Override
    public Object executeRPCEndpoint(String json) throws Exception{
        try {
            RPCActionDto dto = objectMapper.readValue(json, RPCActionDto.class);
            return executeRPCEndpoint(dto, true);
        } catch (JsonProcessingException e) {
            SimpleLogger.error("Failed to extract the json: " + e.getMessage());
        }
        return null;
    }

    /**
     * execute a RPC request with specified client
     * @param client is the client to execute the endpoint
     * @param endpoint is the endpoint to be executed
     */
    private Object executeRPCEndpoint(Object client, EndpointSchema endpoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        if (endpoint.getRequestParams().isEmpty()){
            Method method = client.getClass().getDeclaredMethod(endpoint.getName());
            return method.invoke(client);
        }

        Object[] params = new Object[endpoint.getRequestParams().size()];
        Class<?>[] types = new Class<?>[endpoint.getRequestParams().size()];


        try{
            for (int i = 0; i < params.length; i++){
                NamedTypedValue param = endpoint.getRequestParams().get(i);
                params[i] = param.newInstance();
                types[i] = param.getType().getClazz();
            }
        } catch (Exception e){
            throw new RuntimeException("ERROR: fail to instance value of input parameters based on dto/schema, msg error:"+e.getMessage());
        }

        Method method = client.getClass().getDeclaredMethod(endpoint.getName(), types);

        return method.invoke(client, params);
    }

    private EndpointSchema getEndpointSchema(RPCActionDto dto){
        InterfaceSchema interfaceSchema = rpcInterfaceSchema.get(dto.interfaceId);
        EndpointSchema endpointSchema = interfaceSchema.getOneEndpoint(dto).copyStructure();
        endpointSchema.setValue(dto);
        return endpointSchema;
    }

    private RPCType getRPCType(RPCActionDto dto){
        return rpcInterfaceSchema.get(dto.interfaceId).getRpcType();
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
     * Basic or cookie-based (using {@link org.evomaster.client.java.controller.api.dto.auth.LoginEndpointDto}).
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
     * @deprecated this is now set in DbSpecification
     */
    @Deprecated
    public final Connection getConnection(){
        throw new IllegalStateException("This deprecated method should never be called");
    }

    /**
     * If the system under test (SUT) uses a SQL database, we need to specify
     * the driver used to connect, eg. {@code org.h2.Driver}.
     * This is needed for when we intercept SQL commands with P6Spy
     *
     * @return {@code null} if the SUT does not use any SQL database
     * @deprecated this method is no longer needed
     */
    @Deprecated
    public final String getDatabaseDriverName(){
        throw new IllegalStateException("This deprecated method should never be called");
    }

    public abstract List<TargetInfo> getTargetInfos(Collection<Integer> ids);

    public abstract List<TargetInfo> getAllCoveredTargetInfos();


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

    public abstract void setExecutingInitSql(boolean executingInitSql);

    public abstract void setExecutingInitMongo(boolean executingInitMongo);

    public abstract void setExecutingAction(boolean executingAction);

    public abstract BootTimeInfoDto getBootTimeInfoDto();

    protected BootTimeInfoDto getBootTimeInfoDto(BootTimeObjectiveInfo info){
        if (info == null)
            return null;

        BootTimeInfoDto infoDto = new BootTimeInfoDto();
        infoDto.targets = info.getObjectiveCoverageAtSutBootTime()
                .entrySet().stream().map(e-> new TargetInfoDto(){{
                    descriptiveId = e.getKey();
                    value = e.getValue();
                }}).collect(Collectors.toList());

        infoDto.hostnameResolutionInfoDtos = info.getHostnameInfos().stream()
                .map(h -> new HostnameResolutionInfoDto(h.getHostname(), h.getResolvedAddress())).collect(Collectors.toList());
        infoDto.externalServicesDto = info.getExternalServiceInfo().stream()
                .map(e -> new ExternalServiceInfoDto(e.getProtocol(), e.getHostname(), e.getRemotePort()))
                .collect(Collectors.toList());
        return infoDto;
    }

    public abstract void getJvmDtoSchema(List<String> dtoNames);


    /**
     * mock object might not be loaded when extracting schema with client library
     * after the SUT is started, we attempt to expand mock objects if needed,
     * eg, handle generic types, unidentified DTO class
     */
    private void expandMockObjectIfNeeded(RPCActionDto dto, ActionResponseDto responseDto){
        AtomicBoolean anyUpdate = new AtomicBoolean(false);
        InterfaceSchema schema = rpcInterfaceSchema.get(dto.interfaceId);

        if (dto.mockDatabaseDtos != null && (!dto.mockDatabaseDtos.isEmpty())){
            Stream<MockDatabaseDto> dbstream = dto.mockDatabaseDtos
                .stream()
                .filter(s-> s.responseFullTypeWithGeneric == null);
            dbstream.forEach(s-> anyUpdate.set(buildDbExternalServiceResponse(schema, s, schema.getRpcType()) != null));
        }

        if (dto.mockRPCExternalServiceDtos != null && (!dto.mockRPCExternalServiceDtos.isEmpty())){
            Stream<MockRPCExternalServiceDto> exstream = dto.mockRPCExternalServiceDtos
                .stream()
                .filter(s-> s.responseFullTypesWithGeneric == null);
            exstream.forEach(s-> anyUpdate.set(buildExternalServiceResponse(schema, s, schema.getRpcType()) != null));
        }
        if (anyUpdate.get()){
            ExpandRPCInfoDto expand = new ExpandRPCInfoDto();
            expand.schemaDto = schema.getDto();
            expand.expandActionDto = dto.copy();
            responseDto.expandInfo = expand;
        }
    }

//    private void handleMissingDto(RPCActionDto dto, ActionResponseDto response){
//        if (dto.missingDto != null && !dto.missingDto.isEmpty()){
//            InterfaceSchema schema = rpcInterfaceSchema.get(dto.interfaceId);
//            if (schema != null){
//                buildExternalServiceResponse(schema,
//                    dto.missingDto,
//                    schema.getRpcType());
//                Map<String, NamedTypedValue> types = schema.getObjParamCollections();
//
//                if (dto.missingDto.stream().anyMatch(s-> types.containsKey(s))){
//                    response.latestSchemaDto = schema.getDto();
//                }
//            }
//        }
//    }

    private void extractTypesAndRelated(Map<String, NamedTypedValue> all, List<String> typesToExtract, Map<String, ParamDto> results){
        for (String type : typesToExtract){
            extractTypeAndRelated(all, type, results);
        }
    }

    private void extractTypeAndRelated(Map<String, NamedTypedValue> all, String typeName, Map<String, ParamDto> results){
        if (results.containsKey(typeName)) return;
        NamedTypedValue type = all.get(typeName);
        if (type != null){
            results.put(typeName, type.getDto());
            List<String> referenceTypes = type.referenceTypes();
            if (referenceTypes != null && !referenceTypes.isEmpty()){
                for (String refType : referenceTypes){
                    extractTypeAndRelated(all, refType, results);
                }
            }
        }
    }

    public abstract String getExecutableFullPath();

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
        dto.extractedSpecifiedDtos = recorder.getExtractedSpecifiedDtos();
        dto.numberOfInstrumentedNumberComparisons = recorder.getNumberOfInstrumentedNumberComparisons();
        dto.extraDatabaseConstraintsDtos = recorder.getJpaConstraints().stream()
                .map(c -> {
                    ElementConstraintsDto ec = new ElementConstraintsDto();
                    ec.isNullable = c.getNullable();
                    ec.isOptional = c.getOptional();
                    ec.maxValue = c.getMaxValue();
                    ec.minValue = c.getMinValue();
                    ec.isNotBlank = c.getNotBlank();
                    ec.isEmail = c.getIsEmail();
                    ec.decimalMinValue = c.getDecimalMinValue();
                    ec.decimalMaxValue = c.getDecimalMaxValue();
                    ec.isNegative = c.getIsNegative();
                    ec.isNegativeOrZero = c.getIsNegativeOrZero();
                    ec.isPositive = c.getIsPositive();
                    ec.isPositiveOrZero = c.getIsPositiveOrZero();
                    ec.isFuture = c.getIsFuture();
                    ec.isFutureOrPresent = c.getIsFutureOrPresent();
                    ec.isPast = c.getIsPast();
                    ec.isPastOrPresent = c.getIsPastOrPresent();
                    ec.isAlwaysNull = c.getIsAlwaysNull();
                    ec.patternRegExp = c.getPatternRegExp();
                    ec.sizeMin = c.getSizeMin();
                    ec.sizeMax = c.getSizeMax();
                    ec.digitsFraction = c.getDigitsFraction();
                    ec.digitsInteger = c.getDigitsInteger();
                    ec.enumValuesAsStrings = c.getEnumValuesAsStrings() == null ? null : new ArrayList<>(c.getEnumValuesAsStrings());
                    ExtraConstraintsDto jpa = new ExtraConstraintsDto();
                    jpa.tableName = c.getTableName();
                    jpa.columnName = c.getColumnName();
                    jpa.constraints = ec;
                    return jpa;
                }).collect(Collectors.toList());

        return dto;
    }

    @Override
    public Object getRPCClient(String interfaceName) {
        if (!(getProblemInfo() instanceof RPCProblem))
            throw new RuntimeException("ERROR: the problem should be RPC but it is "+ getProblemInfo().getClass().getSimpleName());

        Object client = ((RPCProblem) getProblemInfo()).getClient(interfaceName);
        if (client == null)
            throw new RuntimeException("ERROR: cannot find any client with the name :"+ interfaceName);

        return client;
    }

    @Override
    public CustomizedCallResultCode categorizeBasedOnResponse(Object response) {
        return null;
    }

    @Override
    public List<CustomizedRequestValueDto> getCustomizedValueInRequests() {
        return null;
    }

    @Override
    public List<CustomizedNotNullAnnotationForRPCDto> specifyCustomizedNotNullAnnotation() {
        return null;
    }

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {
        return null;
    }

    @Override
    public boolean customizeRPCTestOutput(List<MockRPCExternalServiceDto> externalServiceDtos, List<String> sqlInsertions, List<EvaluatedRPCActionDto> actions) {
        return false;
    }

    @Override
    public boolean customizeMockingRPCExternalService(List<MockRPCExternalServiceDto> externalServiceDtos, boolean enabled) {
        return false;
    }

    @Override
    public boolean customizeMockingDatabase(List<MockDatabaseDto> databaseDtos, boolean enabled) {
        return false;
    }

    @Override
    public void resetDatabase(List<String> tablesToClean) {

        if (getDbSpecifications()!= null && !getDbSpecifications().isEmpty()){
            getDbSpecifications().forEach(spec->{
                if (spec==null || spec.connection == null || !spec.employSmartDbClean){
                    return;
                }

                if(tablesToClean == null){
                    // all data will be reset
                    DbCleaner.clearDatabase(spec.connection, null, null, null, spec.dbType);
                    try {
                        reAddAllInitSql();
                    } catch (SQLException e) {
                        throw new RuntimeException("Fail to process all specified initSqlScript "+e);
                    }
                    return;
                }

                if (tablesToClean.isEmpty()) return;

                if (spec.schemaNames == null || spec.schemaNames.isEmpty())
                    DbCleaner.clearDatabase(spec.connection, null, null, tablesToClean, spec.dbType);
                else
                    spec.schemaNames.forEach(sp-> DbCleaner.clearDatabase(spec.connection, sp, null, tablesToClean, spec.dbType));

                try {
                    handleInitSqlInDbClean(tablesToClean, spec);
                } catch (SQLException e) {
                    throw new RuntimeException("Fail to execute the specified initSqlScript "+e);
                }

            });
        }
    }

    /**
     *  Comma , separated list of package prefixes of classes to skip.
     *  This is mainly used as workaround for cases in which EM's instrumentation crashes due
     *  to some bugs in it.
     *  (This is also the reason why it is not abstract)
     *  Note: we currently cannot test this in a E2E, as agent is loaded _before_ te controller is defined
     */
    public String packagesToSkipInstrumentation(){
        return null;
    }


    /**
     * <p>
     *     a method to reset mocked external services with customized method
     * </p>
     */
    @Override
    public final boolean resetCustomizedMethodForMockObject(){
        if (getProblemInfo() instanceof RPCProblem){
            boolean ok = mockRPCExternalServicesWithCustomizedHandling(null, false);
            ok = ok && mockDatabasesWithCustomizedHandling(null, false);
            return ok;
        }
        return false;
    }

    /**
     * <p>
     *     a method to employ customized mocking of RPC based external services
     * </p>
     * @param externalServiceDtos contains info about how to setup responses with json format, note that the json should
     *                            be able to be converted to a list of MockRPCExternalServiceDto
     * @param enabled reflect to enable (set it true) or disable (set it false) the specified external service dtos.
     *                Note that null [externalServiceDtos] with false [enabled] means that all existing external service setup should be disabled.
     * @return whether the mocked instance starts successfully,
     */
    @Override
    public final boolean mockRPCExternalServicesWithCustomizedHandling(String externalServiceDtos, boolean enabled){
        List<MockRPCExternalServiceDto> exDto = null;
        try {
            if (externalServiceDtos != null && !externalServiceDtos.isEmpty())
                exDto = objectMapper.readValue(externalServiceDtos, new TypeReference<List<MockRPCExternalServiceDto>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fail to handle the given external service dto with the info:", e);
        }

        return customizeMockingRPCExternalService(exDto, enabled);
    }

    @Override
    public boolean mockDatabasesWithCustomizedHandling(String mockDatabaseObjectDtos, boolean enabled) {
        List<MockDatabaseDto> mockDbObject = null;
        try {
            if (mockDatabaseObjectDtos != null && !mockDatabaseObjectDtos.isEmpty()) {
                mockDbObject = objectMapper.readValue(mockDatabaseObjectDtos, new TypeReference<List<MockDatabaseDto>>(){});
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fail to handle the given mock object for database with the info:", e);
        }
        return customizeMockingDatabase(mockDbObject, enabled);
    }

    /**
     *
     * @param fileName the name of file which exist in the same directory of the class
     * @return content of file with the specified file
     */
    public final String readFileAsStringFromTestResource(String fileName){
        return (new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(fileName)))))
                .lines().collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public Map<Class, Integer> getExceptionImportanceLevels() {
        return null;
    }
}
