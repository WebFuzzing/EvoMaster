using System;

namespace EvoMaster.Controller.Api {
    public class SutRunDto {
        public SutRunDto() { }

        public SutRunDto(bool? run, bool? resetState, bool? calculateSqlHeuristics, bool? extractSqlExecutionInfo) {
            if (calculateSqlHeuristics.HasValue && calculateSqlHeuristics.Value && extractSqlExecutionInfo.HasValue &&
                !extractSqlExecutionInfo.Value)
                throw new ArgumentException(
                    "extractSqlExecutionInfo should be enabled when calculateSqlHeuristics is enabled");

            this.Run = run;
            this.ResetState = resetState;
            this.CalculateSqlHeuristics = calculateSqlHeuristics;
            this.ExtractSqlExecutionInfo = extractSqlExecutionInfo;
        }

        public SutRunDto(bool? run, bool? resetState, bool? calculateSqlHeuristics) :
            this(run, resetState, calculateSqlHeuristics,
                calculateSqlHeuristics.HasValue && calculateSqlHeuristics.Value) { }

        /**
     * Whether the SUT should be running
     */
        public bool? Run { get; set; }

        /**
     * Whether the internal state of the SUT should be reset
     */
        public bool? ResetState { get; set; }

        /**
     *  Whether SQL heuristics should be computed.
     *  Note: those can be very expensive
     */
        public bool? CalculateSqlHeuristics { get; set; }

        /**
     *  Whether SQL execution info should be saved.
     */
        public bool? ExtractSqlExecutionInfo { get; set; }
    }
}