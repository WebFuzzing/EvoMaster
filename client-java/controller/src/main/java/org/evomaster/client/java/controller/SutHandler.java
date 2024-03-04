package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.DbSpecification;

import java.util.List;

/**
 * Base interface used to control the System Under Test (SUT)
 * from the generated tests.
 * Needed base functionalities are for example, starting/stopping
 * the SUT, and reset its state.
 */
public interface SutHandler {

    /**
     * There might be different settings based on when the SUT is run during the
     * search of EvoMaster, and when it is later started in the generated tests.
     */
    default void setupForGeneratedTest(){}

    /**
     * <p>
     * Start a new instance of the SUT.
     * </p>
     *
     * <p>
     * This method must be blocking until the SUT is initialized.
     *</p>
     *
     * <p>
     * How this method is implemented depends on the library/framework in which
     * the application is written.
     * For example, in Spring applications you can use something like:
     * {@code SpringApplication.run()}
     * </p>
     *
     *
     * @return the base URL of the running SUT, eg "http://localhost:8080"
     */
    String startSut();

    /**
     * <p>
     * Stop the SUT.
     * </p>
     *
     * <p>
     * How to implement this method depends on the library/framework in which
     * the application is written.
     * For example, in Spring applications you can save in a variable the {@code ConfigurableApplicationContext}
     * returned when starting the application, and then call {@code stop()} on it here.
     * </p>
     */
    void stopSut();

    /**
     * <p>
     * Make sure the SUT is in a clean state (eg, reset data in database).
     * </p>
     *
     * <p>
     * A possible (likely very inefficient) way to implement this would be to
     * call {@code stopSUT} followed by {@code startSUT}.
     * </p>
     *
     * <p>
     * When dealing with databases, you can look at the utility functions from
     * the class {@link DbCleaner}.
     * How to access the database depends on the application.
     * To access a {@code java.sql.Connection}, in Spring applications you can use something like:
     * {@code ctx.getBean(JdbcTemplate.class).getDataSource().getConnection()}.
     * </p>
     */
    void resetStateOfSUT();

    /**
     * Execute the given data insertions into the database (if any)
     *
     * @param insertions DTOs for each insertion to execute
     * @param previous an array of insertion results which were executed before this execution
     * @return insertion execution results
     */
    InsertionResultsDto execInsertionsIntoDatabase(List<InsertionDto> insertions, InsertionResultsDto... previous);

    MongoInsertionResultsDto execInsertionsIntoMongoDatabase(List<MongoInsertionDto> insertions);

    /**
     * <p>
     * return an instance of a client of an RPC service.
     * </p>
     *
     * <p>
     * This method must be blocking until the SUT is initialized.
     * </p>
     *
     * <p>
     * This method is only required when the problem is RPC for the moment,
     * otherwise return null
     * </p>
     *
     * might change string interfaceName to class interface
     *
     * @param interfaceName a full name of an interface
     * @return a client which could send requests to the interface
     */
    default Object getRPCClient(String interfaceName){return null;}

    /**
     * <p>
     * execute an RPC endpoint with evomaster driver
     * </p>
     *
     *
     * @param json contains info of an RPC endpoint
     * @return value returned by this execution. it is nullable.
     */
    default Object executeRPCEndpoint(String json) throws Exception {return null;}

    /**
     * <p>
     * execute an RPC endpoint with evomaster driver
     * </p>
     *
     * TODO remove this later if we do not use test generation with driver
     */
    default void extractRPCSchema(){}


    /**
     * <p>
     *     authentication setup might be handled locally.
     *     then we provide this interface to define it.
     * </p>
     *
     * @param authenticationInfo info for the authentication setup
     * @return if the authentication is set up successfully
     */
    default boolean handleLocalAuthenticationSetup(String authenticationInfo){return true;}

    /**
     * <p>
     * If the system under test (SUT) uses a SQL database, we need to have a
     * configured DbSpecification to access/reset it.
     * </p>
     *
     * <p>
     * When accessing a {@code Connection} object to reset the state of
     * the application, we suggest to save it to field (eg when starting the
     * application), and set such field with {@link DbSpecification#connection}.
     * This connection object will be used by EvoMaster to analyze the state of
     * the database to create better test cases.
     * </p>
     *
     * <p>
     * To handle db in the context of testing, there might be a need to initialize
     * data into database with a sql script. such info could be specified with
     * {@link DbSpecification#dbType}
     * </p>
     *
     * <p>
     * With EvoMaster, we also support a smart DB cleaner by removing all data in tables
     * which has been accessed after each test. In order to achieve this, we requires user
     * to set a set of info such as database type with {@link DbSpecification#dbType},
     * schema name with {@link DbSpecification#schemaNames} (TODO might remove later).
     * In addition, we also provide an option (default is {@code true}) to configure
     * if such cleaner is preferred with {@link DbSpecification#employSmartDbClean}.
     * </p>
     *
     * @return {@code null} if the SUT does not use any SQL database
     */

    List<DbSpecification> getDbSpecifications();

    default Object getMongoConnection() {return null;}


    /**
     * <p>
     * register or execute specified SQL script for initializing data in database
     * </p>
     */
    default void registerOrExecuteInitSqlCommandsIfNeeded(){}

    /**
     * <p>
     * reset database if the smart db cleaning is employed
     * </p>
     * @param tablesToClean represents a list of table which will be reset based on specified DbSpecification.
     *                      note that null tablesToClean means all table will be reset.
     */
    default void resetDatabase(List<String> tablesToClean){}

    /**
     * <p>
     *     a method to reset mocked external services with customized method
     * </p>
     */
    default boolean resetCustomizedMethodForMockObject(){
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
    default boolean mockRPCExternalServicesWithCustomizedHandling(String externalServiceDtos, boolean enabled){
        return false;
    }


    /**
     * <p>
     *     a method to employ customized mocking for database
     * </p>
     * @param mockDatabaseObjectDtos contains info about how to set up mock object for databases with json format, note that the json should
     *                            be able to be converted to a list of MockDatabaseDto
     * @param enabled reflect to enable (set it true) or disable (set it false) the specified mock object
     *                Note that null [mockDatabaseObjectDtos] with false [enabled] means that all existing mock objects for databases should be disabled.
     * @return whether the mocked instance starts successfully,
     */
    default boolean mockDatabasesWithCustomizedHandling(String mockDatabaseObjectDtos, boolean enabled){
        return false;
    }

}
