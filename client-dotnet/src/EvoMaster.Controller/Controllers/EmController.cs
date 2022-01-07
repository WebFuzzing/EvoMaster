using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using EvoMaster.Client.Util;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Problem;
using EvoMaster.Instrumentation.StaticState;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace EvoMaster.Controller.Controllers {
    public class EmController : ControllerBase {
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
        private static readonly ICollection<string> ConnectedClientsSoFar = new SynchronizedCollection<string>();

        //The html file gets copied inside the SUT's bin folder after build
        private static readonly string HtmlWarning; // =
        // System.IO.File.ReadAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "warning.html"));


        private readonly object _syncLock = new object();

        static EmController() {
            var assembly = Assembly.GetAssembly(typeof(EmController));
            if (assembly == null) {
                SimpleLogger.Warn("Assembly of EmController not found. warning.html couldn't be fetched.");
                return;
            }

            var resourceStream = assembly.GetManifestResourceStream("EvoMaster.Controller.Resources.warning.html");
            if (resourceStream == null) return;
            using var reader = new StreamReader(resourceStream, Encoding.UTF8);
            HtmlWarning = reader.ReadToEnd();
        }


        public EmController(SutController sutController) {
            if (!sutController.Equals(null))
                _sutController = sutController;
            else
                throw new NullReferenceException("SutController shouldn't be null");
        }

        private bool TrackRequestSource(ConnectionInfo connectionInfo) {
            var source = $"{connectionInfo.RemoteIpAddress}:{connectionInfo.RemotePort}";

            ConnectedClientsSoFar.Add(source);

            return true;
        }

        private void AssertTrackRequestSource(ConnectionInfo connectionInfo) {
            var res = TrackRequestSource(connectionInfo);

            if (!res) throw new InvalidOperationException();
        }

        //Only used for debugging/testing
        ///<summary>Returns host:port of all clients connected so far</summary>
        public static ISet<string> GetConnectedClientsSoFar() => ConnectedClientsSoFar.ToHashSet();

        //Only used for debugging/testing
        public static void ResetConnectedClientsSoFar() => ConnectedClientsSoFar.Clear();

        [HttpGet("")]
        public IActionResult GetWarning() => new ContentResult {
            ContentType = "text/html",
            StatusCode = StatusCodes.Status400BadRequest,
            Content = HtmlWarning
        };

        [HttpGet("controller/api/infoSUT")]
        public IActionResult GetSutInfo() {
            string connectionHeader = Request.Headers["Connection"];

            if (connectionHeader == null ||
                !connectionHeader.Equals("keep-alive", StringComparison.OrdinalIgnoreCase)) {
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

            var dto = new SutInfoDto();
            dto.IsSutRunning = _sutController.IsSutRunning();
            dto.BaseUrlOfSUT = _baseUrlOfSut;
            dto.InfoForAuthentication = _sutController.GetInfoForAuthentication();
            //TODO: uncomment
            //dto.SqlSchemaDto = _sutController.GetSqlDatabaseSchema ();
            dto.DefaultOutputFormat = _sutController.GetPreferredOutputFormat();

            var info = _sutController.GetProblemInfo();

            if (info == null) {
                var msg = "Undefined problem type in the EM Controller";

                SimpleLogger.Error(msg);

                return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            }

            if (info is RestProblem rp) {
                dto.RestProblem = new RestProblemDto {
                    OpenApiUrl = rp.GetOpenApiUrl(),
                    EndpointsToSkip = rp.GetEndpointsToSkip()
                };
            }
            else {
                var msg = "Unrecognized problem type: " + info.GetType().FullName;

                SimpleLogger.Error(msg);

                return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            }

            dto.UnitsInfoDto = _sutController.GetUnitsInfoDto();
            if (dto.UnitsInfoDto == null) {
                var msg = "Failed to extract units info";

                SimpleLogger.Error(msg);

                return StatusCode(StatusCodes.Status500InternalServerError, WrappedResponseDto<string>.WithError(msg));
            }

            return Ok(WrappedResponseDto<SutInfoDto>.WithData(dto));
        }

        [HttpGet("controller/api/controllerInfo")]
        public IActionResult GetControllerInfoDto() {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            var dto = new ControllerInfoDto();
            dto.FullName = _sutController.GetType().FullName;
            dto.IsInstrumentationOn = _sutController.IsInstrumentationActivated();

            return Ok(WrappedResponseDto<ControllerInfoDto>.WithData(dto));
        }

        [HttpPost("controller/api/newSearch")]
        public IActionResult NewSearch() {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            _sutController.NewSearch();

            return StatusCode(StatusCodes.Status201Created);
        }

        //TODO: How to get url from another file
        //TODO: Log errors in web server instead of try-catch 
        [HttpPut("controller/api/runSUT")]
        public IActionResult RunSut([FromBody] SutRunDto dto) {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            if (dto == null || !dto.Run.HasValue) {
                const string errorMessage = "Invalid JSON: 'run' field is required";

                SimpleLogger.Warn(errorMessage);

                return BadRequest(WrappedResponseDto<string>.WithError(errorMessage));
            }

            var sqlHeuristics = dto.CalculateSqlHeuristics != null && dto.CalculateSqlHeuristics.Value;
            var sqlExecution = dto.ExtractSqlExecutionInfo != null && dto.ExtractSqlExecutionInfo.Value;

            //TODO: uncomment
            // _sutController.EnableComputeSqlHeuristicsOrExtractExecution (sqlHeuristics, sqlExecution);

            var doReset = dto.ResetState != null && dto.ResetState.Value;

            lock (_lock) {
                if (!dto.Run.Value) {
                    if (doReset) {
                        var errorMessage = "Invalid JSON: cannot reset state and stop service at same time";

                        SimpleLogger.Warn(errorMessage);

                        return BadRequest(WrappedResponseDto<string>.WithError(errorMessage));
                    }

                    //if on, we want to shut down the server
                    if (_sutController.IsSutRunning()) {
                        _sutController.StopSut();
                        _baseUrlOfSut = null;
                    }
                }
                else {
                    /*
                        If SUT is not up and running, let's start it
                     */
                    if (!_sutController.IsSutRunning()) {
                        _baseUrlOfSut = _sutController.StartSut();
                        if (_baseUrlOfSut == null) {
                            //there has been an internal failure in starting the SUT
                            var msg = "Internal failure: cannot start SUT based on given configuration";

                            SimpleLogger.Warn(msg);

                            return StatusCode(StatusCodes.Status500InternalServerError,
                                WrappedResponseDto<string>.WithError(msg));
                        }

                        //TODO: uncomment
                        // _sutController.InitSqlHandler ();
                    }

                    /*
                        regardless of where it was running or not, need to reset state.
                        this is controlled by a boolean, although most likely we ll always
                        want to do it
                     */
                    if (dto.ResetState.HasValue && dto.ResetState.Value) {
                        try {
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
                        finally {
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
        public IActionResult NewAction([FromBody] ActionDto dto) {
            AssertTrackRequestSource(Request.HttpContext.Connection);

            _sutController.NewAction(dto);

            return NoContent();
        }

        //TODO:complete this method
        [HttpGet("controller/api/testResults")]
        public IActionResult GetTestResults([FromQuery] string ids, [FromQuery] bool killSwitch) {
            if (ids == null) ids = "";

            var idsList = new List<int>();
            try {
                ids.Split(',').Where(x => !string.IsNullOrEmpty(x.Trim())).ToList()
                    .ForEach(x => idsList.Add(Convert.ToInt32(x)));
            }
            catch (Exception e) {
                var msg = "Invalid parameter 'ids': " + e.Message;

                SimpleLogger.Warn(msg);

                return BadRequest(WrappedResponseDto<TestResultsDto>.WithError(msg));
            }

            var dto = new TestResultsDto();

            var targetInfos = NoKillSwitch(() => _sutController.GetTargetInfos(idsList));

            if (targetInfos == null) {
                var msg = "Failed to collect target information for " + ids.Length + " ids";
                SimpleLogger.Error(msg);
                return StatusCode(500, WrappedResponseDto<TestResultsDto>.WithError(msg));
            }

            targetInfos.ToList().ForEach(t => {
                var info = new TargetInfoDto {
                    Id = t.MappedId,
                    Value = t.Value,
                    DescriptiveId = t.DescriptiveId,
                    ActionIndex = t.ActionIndex
                };

                dto.Targets.Add(info);
            });

            /*
                Note: it is important that extra is computed before AdditionalInfo,
                as heuristics on SQL might add new entries to String specializations

                FIXME actually the String specialization would work only on Embedded, and
                not on External :(
                But, as anyway we are going to refactor it in Core at a later point, no need
                to waste time for a tmp workaround
             */

            // dto.ExtraHeuristics = NoKillSwitch(() => _sutController.GetExtraHeuristics()); TODO

            var additionalInfos = NoKillSwitch(() => _sutController.GetAdditionalInfoList().ToList());

            if (additionalInfos != null) {
                additionalInfos.ForEach(a => {
                    var info = new AdditionalInfoDto {
                        QueryParameters = new HashSet<string>(a.GetQueryParametersView()),
                        Headers = new HashSet<string>(a.GetHeadersView()),
                        LastExecutedStatement = a.GetLastExecutedStatement(),
                        RawAccessOfHttpBodyPayload = a.IsRawAccessOfHttpBodyPayload(),
                        ParsedDtoNames = new HashSet<string>(a.GetParsedDtoNamesView()),
                        StringSpecializations = new Dictionary<string, IList<StringSpecializationInfoDto>>()
                    };

                    foreach (var entry in
                             a.GetStringSpecializationsView()) {
                        Trace.Assert(entry.Value.Count != 0);

                        var list = new List<StringSpecializationInfoDto>();

                        entry.Value.ToList()
                            .ForEach(it => {
                                var stringSpecializationInfoDto = new StringSpecializationInfoDto(
                                    it.GetStringSpecialization().ToString(),
                                    it.GetValue(),
                                    it.GetTaintType().ToString());
                                list.Add(stringSpecializationInfoDto);
                            });


                        info.StringSpecializations.Add(entry.Key, list);
                    }

                    dto.AdditionalInfoList.Add(info);
                });
            }
            else {
                const string msg = "Failed to collect additional info";
                SimpleLogger.Error(msg);
                return StatusCode(500, WrappedResponseDto<TestResultsDto>.WithError(msg));
            }

            if (killSwitch) {
                _sutController.SetKillSwitch(true);
            }

            return Ok(WrappedResponseDto<TestResultsDto>.WithData(dto));
        }

        private T NoKillSwitch<T>(Func<T> func) {
            var previous = ExecutionTracer.IsKillSwitch();

            ExecutionTracer.SetKillSwitch(false);

            var t = func();

            ExecutionTracer.SetKillSwitch(previous);

            return t;
        }

        //TODO: implement ExecuteDatabaseCommand method
    }
}