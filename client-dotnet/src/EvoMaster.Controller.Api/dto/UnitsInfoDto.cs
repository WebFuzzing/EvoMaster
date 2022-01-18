using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    /**
   * Information about the "units" in the SUT.
   * In case of OO languages like Java and Kotlin, those will be "classes"
   *
   * Created by arcuri82 on 27-Sep-19.
   */
    public class UnitsInfoDto {
        /**
     * Then name of all the units (eg classes) in the SUT
     */
        public IEnumerable<string> UnitNames { get; set; }

        /**
     * The total number of lines/statements/instructions in all
     * units of the whole SUT
     */
        public int NumberOfLines { get; set; }

        /**
     * The total number of branches in all
     * units of the whole SUT
     */
        public int NumberOfBranches { get; set; }

        /**
     * Number of replaced method testability transformations.
     * But only for SUT units.
     */
        public int NumberOfReplacedMethodsInSut { get; set; }

        /**
     * Number of replaced method testability transformations.
     * But only for third-party library units (ie all units not in the SUT).
     */
        public int NumberOfReplacedMethodsInThirdParty { get; set; }

        /**
     * Number of tracked methods. Those are special methods for which
     * we explicitly keep track of how they are called (eg their inputs).
     */
        public int NumberOfTrackedMethods { get; set; }

        /**
     * Number of cases in which numeric comparisons needed customized
     * instrumentation (eg, needed on JVM for non-integer types)
     */
        public int NumberOfInstrumentedNumberComparisons { get; set; }

        /*
            Key -> DTO full name
            Value -> OpenAPI object schema
            TODO should consider if also adding info on type, eg JSON vs XML
         */
        public IDictionary<string, string> ParsedDtos { get; set; }
    }
}