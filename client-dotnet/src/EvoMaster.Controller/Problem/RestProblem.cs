using System.Collections.Generic;

namespace EvoMaster.Controller.Problem {
  public class RestProblem : IProblemInfo {

    private readonly string swaggerJsonUrl;

    private readonly IList<string> endpointsToSkip;

    ///<param name="swaggerJsonUrl">Provide the URL of where the swagger.json can be found</param>
    ///<param name="endpointsToSkip">When testing a REST API, there might be some endpoints that are not so important to test.
    ///For example, in Spring, health-check endpoints like "/heapdump"
    ///are not so interesting to test, and they can be very expensive to run.
    ///Here can specify a list of endpoints (as defined in the schema) to skip.
    ///</param>
    public RestProblem (string swaggerJsonUrl, IList<string> endpointsToSkip) {
      this.swaggerJsonUrl = swaggerJsonUrl;
      this.endpointsToSkip = endpointsToSkip == null ?
        new List<string> () :
        new List<string> (endpointsToSkip);
    }

    public string GetSwaggerJsonUrl () {
      return swaggerJsonUrl;
    }

    public IList<string> GetEndpointsToSkip () {
      return endpointsToSkip;
    }
  }
}