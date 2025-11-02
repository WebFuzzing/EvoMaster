using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class RestProblemDto {
        /**
         * The full URL of where the Swagger JSON data can be located
         */
        public string OpenApiUrl { get; set; }

        /**
         * When testing a REST API, there might be some endpoints that are not
         * so important to test.
         * For example, in Spring, health-check endpoints like "/heapdump"
         * are not so interesting to test, and they can be very expensive to run.
         */
        public IList<string> EndpointsToSkip { get; set; }
    }
}