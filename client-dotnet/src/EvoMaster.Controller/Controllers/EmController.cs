using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using EvoMaster.Client.Util;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Problem;
using EvoMaster.Instrumentation;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace EvoMaster.Controller.Controllers
{
    public class EmController : ControllerBase
    {
        private readonly SutController _sutController;
        private static string _baseUrlOfSut;
        private readonly object _lock = new object();

        /*
         Keep track of all host:port clients connect so far.
         This is the mainly done for debugging, to check that we are using
         a single TCP connection, instead of creating new ones at each request.
         
         However, we want to check it only during testing
         */
        //TODO: check
        private static readonly ICollection<string> connectedClientsSoFar = new SynchronizedCollection<string>();

        private static readonly SemaphoreLocker _locker = new SemaphoreLocker();

        //The html file gets copied inside the SUT's bin folder after build
        private static readonly string htmlWarning;// =
           // System.IO.File.ReadAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "warning.html"));
           

        private readonly object syncLock = new object();

        static EmController() {

            var assembly = Assembly.GetAssembly(typeof(EmController));
            var resourceStream = assembly.GetManifestResourceStream("EvoMaster.Controller.Resources.warning.html");
            using var reader = new StreamReader(resourceStream, Encoding.UTF8);
            htmlWarning =  reader.ReadToEnd();
        }
        
        
        public EmController(SutController sutController)
        {
            if (!sutController.Equals(null))
                _sutController = sutController;
            else
                throw new NullReferenceException("SutController shouldn't be null");
        }

        private bool TrackRequestSource(ConnectionInfo connectionInfo)
        {
            string source = $"{connectionInfo.RemoteIpAddress}:{connectionInfo.RemotePort}";

            connectedClientsSoFar.Add(source);

            return true;
        }

        private void AssertTrackRequestSource(ConnectionInfo connectionInfo)
        {
            var res = TrackRequestSource(connectionInfo);

            if (!res) throw new InvalidOperationException();
        }

        //Only used for debugging/testing
        ///<summary>Returns host:port of all clients connected so far</summary>
        public static ISet<string> GetConnectedClientsSoFar() => connectedClientsSoFar.ToHashSet();

        //Only used for debugging/testing
        public static void ResetConnectedClientsSoFar() => connectedClientsSoFar.Clear();

        [HttpGet("")]
        public IActionResult GetWarning() => new ContentResult
        {
            ContentType = "text/html",
            StatusCode = StatusCodes.Status400BadRequest,
            Content = htmlWarning
        };

        [HttpGet("controller/api/infoSUT")]
        public IActionResult GetSutInfo()
        {
            string connectionHeader = Request.Headers["Connection"];

            if (connectionHeader == null ||
                !connectionHeader.Equals("keep-alive", StringComparison.OrdinalIgnoreCase))
            {
                return BadRequest(
                    WrappedResponseDto<string>.WithError("Requests should always contain a 'Connection: keep-alive'"));
            }

            AssertTrackRequestSource(Request.HttpContext.Connection);

            //TODO: uncomment after implementing VerifySqlConnection method
            // if (!_sutController.VerifySqlConnection ()) {
            //   string msg = "SQL drivers are misconfigured. You must use a 'p6spy' wrapper when you " +
            //     "run the SUT. For example, a database connection URL like 'jdbc:h2:mem:testdb' " +
            //     "should be changed into 'jdbc:p6spy:h2:mem:testdb'. " +
            //     "See documentation on how to configure P6Spy.";

            //   SimpleLogger.Error (msg);

            //   return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            // }

            SutInfoDto dto = new SutInfoDto();
            dto.IsSutRunning = _sutController.IsSutRunning();
            dto.BaseUrlOfSUT = _baseUrlOfSut;
            dto.InfoForAuthentication = _sutController.GetInfoForAuthentication();
            //TODO: uncomment
            //dto.SqlSchemaDto = _sutController.GetSqlDatabaseSchema ();
            dto.DefaultOutputFormat = _sutController.GetPreferredOutputFormat();

            IProblemInfo info = _sutController.GetProblemInfo();

            if (info == null)
            {
                string msg = "Undefined problem type in the EM Controller";

                SimpleLogger.Error(msg);

                return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            }
            else if (info is RestProblem)
            {
                RestProblem rp = (RestProblem) info;
                dto.RestProblem = new RestProblemDto();
                dto.RestProblem.SwaggerJsonUrl = rp.GetSwaggerJsonUrl();
                dto.RestProblem.EndpointsToSkip = rp.GetEndpointsToSkip();
            }
            else
            {
                string msg = "Unrecognized problem type: " + info.GetType().FullName;

                SimpleLogger.Error(msg);

                return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            }

            dto.UnitsInfoDto = _sutController.GetUnitsInfoDto();
            if (dto.UnitsInfoDto == null)
            {
                string msg = "Failed to extract units info";

                SimpleLogger.Error(msg);

                return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            }

            return Ok(WrappedResponseDto<SutInfoDto>.WithData(dto));
        }

        [HttpGet("controller/api/controllerInfo")]
        public IActionResult GetControllerInfoDto()
        {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            ControllerInfoDto dto = new ControllerInfoDto();
            dto.FullName = _sutController.GetType().FullName;
            dto.IsInstrumentationOn = _sutController.IsInstrumentationActivated();

            return Ok(WrappedResponseDto<ControllerInfoDto>.WithData(dto));
        }

        [HttpPost("controller/api/newSearch")]
        public IActionResult NewSearch()
        {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            _sutController.NewSearch();

            return StatusCode(StatusCodes.Status201Created);
        }

        //TODO: How to get url from another file
        //TODO: Log errors in web server instead of try-catch 
        [HttpPut("controller/api/runSUT")]
        public IActionResult RunSut([FromBody] SutRunDto dto)
        {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            if (dto == null || !dto.Run.HasValue)
            {
                string errorMessage = "Invalid JSON: 'run' field is required";

                SimpleLogger.Warn(errorMessage);

                return BadRequest(WrappedResponseDto<string>.WithError(errorMessage));
            }

            bool sqlHeuristics = dto.CalculateSqlHeuristics != null && dto.CalculateSqlHeuristics.Value;
            bool sqlExecution = dto.ExtractSqlExecutionInfo != null && dto.ExtractSqlExecutionInfo.Value;

            //TODO: uncomment
            // _sutController.EnableComputeSqlHeuristicsOrExtractExecution (sqlHeuristics, sqlExecution);

            bool doReset = dto.ResetState != null && dto.ResetState.Value;

            lock (_lock)
            {
                if (dto.Run.HasValue && !dto.Run.Value)
                {
                    if (doReset)
                    {
                        string errorMessage = "Invalid JSON: cannot reset state and stop service at same time";

                        SimpleLogger.Warn(errorMessage);

                        return BadRequest(WrappedResponseDto<string>.WithError(errorMessage));
                    }

                    //if on, we want to shut down the server
                    if (_sutController.IsSutRunning())
                    {
                        _sutController.StopSut();
                        _baseUrlOfSut = null;
                    }
                }
                else
                {
                    /*
                        If SUT is not up and running, let's start it
                     */
                    if (!_sutController.IsSutRunning())
                    {
                        _baseUrlOfSut = _sutController.StartSut();
                        if (_baseUrlOfSut == null)
                        {
                            //there has been an internal failure in starting the SUT
                            String msg = "Internal failure: cannot start SUT based on given configuration";

                            SimpleLogger.Warn(msg);

                            return StatusCode(StatusCodes.Status500InternalServerError,
                                WrappedResponseDto<string>.WithError(msg));
                        }

                        //TODO: uncomment
                        // _sutController.InitSqlHandler ();
                    }
                    else
                    {
                        //TODO as starting should be blocking, need to check
                        //if initialized, and wait if not
                    }

                    /*
                        regardless of where it was running or not, need to reset state.
                        this is controlled by a boolean, although most likely we ll always
                        want to do it
                     */
                    if (dto.ResetState.HasValue && dto.ResetState.Value)
                    {
                        try
                        {
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
                            _sutController.ResetStateOfSut();
                        }
                        finally
                        {
                            _sutController.NewTest();
                        }
                    }

                    /*
                        Note: here even if we start the SUT, the starting of a "New Search"
                        cannot be done here, as in this endpoint we also deal with the reset
                        of state. When we reset state for a new test run, we do not want to
                        reset all the other data regarding the whole search
                     */
                }

                return StatusCode(StatusCodes.Status204NoContent);
            }
        }

        [HttpPut("controller/api/newAction")]
        [Consumes("application/json")]
        public IActionResult NewAction([FromBody] ActionDto dto)
        {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            _sutController.NewAction(dto);

            return NoContent();
        }

        //TODO:complete this method
        [HttpGet("controller/api/testResults")]
        public IActionResult GetTestResults([FromQuery] string ids)
        {

            //java version: List<AdditionalInfo> additionalInfos = noKillSwitch(() -> sutController.getAdditionalInfoList());
            IList<AdditionalInfo> additionalInfos = _sutController.GetAdditionalInfoList();

            //Fake data here
            var dto = new TestResultsDto
            {
                AdditionalInfoList = Enumerable.Repeat(new AdditionalInfoDto
                {
                    LastExecutedStatement = "\"TODO: LastExecutedStatement\""
                }, additionalInfos.Count).ToList()
            };

            return Ok(WrappedResponseDto<TestResultsDto>.WithData(dto));
        }

        //TODO: implement ExecuteDatabaseCommand method
    }
}