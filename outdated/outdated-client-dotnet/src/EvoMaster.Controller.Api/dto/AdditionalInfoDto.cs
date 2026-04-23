using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class AdditionalInfoDto {
        /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
        public ISet<string> QueryParameters { get; set; } = new HashSet<string>();

        /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
        public ISet<string> Headers { get; set; } = new HashSet<string>();

        /**
     * Information for taint analysis.
     * When some string inputs are recognized of a specific type (eg,
     * they are used as integers or dates), we keep track of it.
     * The key in this map is the value of the tainted input.
     * The associated list is its possible specializations (which usually
     * will be at most 1).
     */
        public IDictionary<string, IList<StringSpecializationInfoDto>> StringSpecializations { get; set; } =
            new Dictionary<string, IList<StringSpecializationInfoDto>>();

        /**
     * Keep track of the last executed statement done in the SUT.
     * But not in the third-party libraries, just the business logic of the SUT.
     * The statement is represented with a descriptive unique id, like the class name and line number.
     */
        public string LastExecutedStatement { get; set; }

        /**
     * Check if the business logic of the SUT (and not a third-party library) is
     * accessing the raw bytes of HTTP body payload (if any) directly
     */
        public bool? RawAccessOfHttpBodyPayload { get; set; }

        /**
     * The name of all DTO that have been parsed (eg, with GSON and Jackson).
     * Note: the actual content of schema is queried separately.
     */
        public ISet<string> ParsedDtoNames { get; set; } = new HashSet<string>();
    }
}