using System.Collections.Generic;

namespace EvoMaster.Controller.Problem {
    public class RestProblem : IProblemInfo {
        private readonly string _openApiUrl;

        private readonly IList<string> _endpointsToSkip;

        ///<param name="openApiUrl">Provide the URL of where the swagger.json can be found</param>
        ///<param name="endpointsToSkip">When testing a REST API, there might be some endpoints that are not so important to test.
        ///For example, in Spring, health-check endpoints like "/heapdump"
        ///are not so interesting to test, and they can be very expensive to run.
        ///Here can specify a list of endpoints (as defined in the schema) to skip.
        ///</param>
        public RestProblem(string openApiUrl, IList<string> endpointsToSkip) {
            this._openApiUrl = openApiUrl;
            this._endpointsToSkip = endpointsToSkip == null ? new List<string>() : new List<string>(endpointsToSkip);
        }

        public string GetOpenApiUrl() {
            return _openApiUrl;
        }

        public IList<string> GetEndpointsToSkip() {
            return _endpointsToSkip;
        }
    }
}