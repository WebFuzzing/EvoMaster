using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using EvoMaster.Client.Util;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Problem;
using EvoMaster.Instrumentation;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace EvoMaster.Controller {
    ///<summary>
    ///Abstract class used to connect to the EvoMaster process, and
    ///that is responsible to start/stop/restart the tested application,
    ///ie the system under test (SUT)
    ///</summary>
    public abstract class SutController : ISutHandler {
        private int _controllerPort = ControllerConstants.DefaultControllerPort;
        private string _controllerHost = ControllerConstants.DefaultControllerHost;

        //TODO: To be added
        //private final SqlHandler sqlHandler = new SqlHandler();

        ///<summary>
        ///If using a SQL Database, gather info about its schema
        ///</summary>
        //TODO: Commented this out just to prevent warning
        // private DbSchemaDto SchemaDto;

        //For each action in a test, keep track of the extra heuristics, if any
        private readonly ICollection<ExtraHeuristicsDto> _extras = new SynchronizedCollection<ExtraHeuristicsDto>();

        //TODO: Commented this out just to prevent warning
        private int _actionIndex = -1;

        public abstract void ResetStateOfSut();

        public abstract string StartSut();

        public abstract void StopSut();

        ///<summary>
        ///Start the controller as a RESTful server.
        ///Use the setters of this class to change the default
        ///port and host.
        ///</summary>
        ///<remarks>This method is blocking until the server is initialized.</remarks>
        ///<returns>returns true if there was no problem in starting the controller </returns>
        public bool StartTheControllerServer() {
            try {
                CreateHostBuilder().Build().Run();
            }
            catch (System.Exception e) {
                SimpleLogger.Error("Failed to start web server", e);

                return false;
            }

            return true;
        }

        public bool StopTheControllerServer() {
            //TODO: complete this method
            throw new NotImplementedException();
        }

        ///<summary>Returns the actual port in use (eg, if it was an ephemeral 0)</summary>
        public int GetControllerServerPort() {
            //TODO: Complete this
            throw new NotImplementedException();
            //return ((AbstractNetworkConnector) controllerServer.getConnectors () [0]).getLocalPort ();
        }

        public int GetControllerPort() {
            return _controllerPort;
        }

        public void SetControllerPort(int controllerPort) {
            this._controllerPort = controllerPort;
        }

        public string GetControllerHost() {
            return _controllerHost;
        }

        public void SetControllerHost(string controllerHost) {
            this._controllerHost = controllerHost;
        }

        //TODO: Complete this method
        public void ExecInsertionsIntoDatabase(IList<InsertionDto> insertions) {
            throw new NotImplementedException();
        }

        ///<summary>Calculate heuristics based on intercepted SQL commands</summary>
        ///<param name="sql">command as a string</param>
        //TODO: Complete this method
        public void HandleSql(string sql) {
            throw new NotImplementedException();
        }

        //TODO: Complete this method
        public void EnableComputeSqlHeuristicsOrExtractExecution(bool enableSqlHeuristics, bool enableSqlExecution) {
            throw new NotImplementedException();
        }

        ///<summary>
        ///This is needed only during test generation (not execution),
        ///and it is automatically called by the EM controller after the SUT is started.
        ///</summary>
        //TODO: Complete this method
        public void InitSqlHandler() {
            throw new NotImplementedException();
        }

        //TODO: Complete this method
        public void ResetExtraHeuristics() {
            //throw new NotImplementedException();
        }

        //TODO: Complete this method
        public IList<ExtraHeuristicsDto> GetExtraHeuristics() {
            throw new NotImplementedException();
        }

        //TODO: Complete this method
        public ExtraHeuristicsDto ComputeExtraHeuristics() {
            throw new NotImplementedException();
        }

        /**
         * Extra information about the SQL Database Schema, if any is present.
         * Note: this is extracted by querying the database itself.
         * So the database must be up and running.
         *
         * @return a DTO with the schema information
         * @see SutController#getConnection
         */
        //TODO: Complete this method
        public DbSchemaDto GetSqlDatabaseSchema() {
            throw new NotImplementedException();
        }

        /**
         * Either there is no connection, or, if there is, then it must have P6Spy configured.
         * But this might not apply to all kind controllers
         *
         * @return false if the verification failed
         */
        //TODO: Complete this method
        public bool VerifySqlConnection() {
            throw new NotImplementedException();
        }

        /**
         * Re-initialize some internal data needed before running a new test
         *
         * Man: I modified this, please check Amid.
         */
        public void NewTest() {
            _actionIndex = -1;

             ResetExtraHeuristics();
             _extras.Clear();
            NewTestSpecificHandler();
        }
        
        
        /**
         * As some heuristics are based on which action (eg HTTP call, or click of button)
         * in the test sequence is executed, and their order, we need to keep track of which
         * action does cover what.
         *
         * @param dto the DTO with the information about the action (eg its index in the test)
         */
        //TODO: Complete this method. Man: modified, please check
        public void NewAction(ActionDto dto) {
            if (dto.Index > _extras.Count) {
                //_extras.Add(ComputeExtraHeuristics());
            }
            this._actionIndex = dto.Index;
            
            ResetExtraHeuristics();

            NewActionSpecificHandler(dto);
        }

        /**
         * Re-initialize all internal data to enable a completely new search phase
         * which should be independent from previous ones
         */
        public abstract void NewSearch();

        public abstract void NewTestSpecificHandler();

        public abstract void NewActionSpecificHandler(ActionDto dto);

        /**
         * Check if bytecode instrumentation is on.
         *
         * @return true if the instrumentation is on
         */
        public abstract bool IsInstrumentationActivated();

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
        public abstract bool IsSutRunning();

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
        public abstract string GetPackagePrefixesToCover();

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
        public abstract List<AuthenticationDto> GetInfoForAuthentication();

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
        //TODO: Complete this method
        // public abstract Connection GetConnection ();

        /**
         * If the system under test (SUT) uses a SQL database, we need to specify
         * the driver used to connect, eg. {@code org.h2.Driver}.
         * This is needed for when we intercept SQL commands with P6Spy
         *
         * @return {@code null} if the SUT does not use any SQL database
         */
        public abstract string GetDatabaseDriverName();

        //TODO: Complete this method
        public abstract IList<TargetInfo> GetTargetInfos(IEnumerable<int> ids);

        /**
         * @return additional info for each action in the test.
         * The list is ordered based on the action index.
         */
        //TODO: Complete this method
        public abstract IList<AdditionalInfo> GetAdditionalInfoList();

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
        public abstract IProblemInfo GetProblemInfo();

        /**
         * Test cases could be outputted in different language (e.g., Java and Kotlin),
         * using different testing libraries (e.g., JUnit 4 or 5).
         * Here, need to specify the default option.
         *
         * @return the format in which the test cases should be generated
         */
        public abstract OutputFormat GetPreferredOutputFormat();

        public abstract UnitsInfoDto GetUnitsInfoDto();

        protected UnitsInfoDto GetUnitsInfoDto(UnitsInfoRecorder recorder) {
            if (recorder == null) {
                return null;
            }

            var dto = new UnitsInfoDto {
                NumberOfBranches = recorder.GetNumberOfBranches(),
                NumberOfLines = recorder.GetNumberOfLines(),
                NumberOfReplacedMethodsInSut = recorder.GetNumberOfReplacedMethodsInSut(),
                NumberOfReplacedMethodsInThirdParty = recorder.GetNumberOfReplacedMethodsInThirdParty(),
                NumberOfTrackedMethods = recorder.GetNumberOfTrackedMethods(),
                UnitNames = recorder.GetUnitNames(),
                ParsedDtos = recorder.GetParsedDtos(),
                NumberOfInstrumentedNumberComparisons = recorder.GetNumberOfInstrumentedNumberComparisons()
            };
            return dto;
        }

        public abstract void SetKillSwitch(bool b);

        private IHostBuilder CreateHostBuilder() =>
            Host.CreateDefaultBuilder()
                .ConfigureServices((hc, services) => {
                    services.Add(ServiceDescriptor.Singleton(typeof(SutController), this));
                })
                .ConfigureWebHostDefaults(webBuilder => {
                    webBuilder.UseStartup<Startup>().UseUrls($"http://*:{_controllerPort}");
                });

        protected int GetEphemeralTcpPort() {
            var tcpListener = new TcpListener(IPAddress.Loopback, 0);

            tcpListener.Start();

            var port = ((IPEndPoint) tcpListener.LocalEndpoint).Port;

            tcpListener.Stop();

            return port;
        }
    }
}