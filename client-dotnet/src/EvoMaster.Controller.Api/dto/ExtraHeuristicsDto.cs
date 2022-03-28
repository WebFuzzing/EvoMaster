using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    /**
   * Represents possible extra heuristics related to the code
   * execution and that do apply to all the reached testing targets.
   *
   * Example: rewarding SQL "select" operations that return non-empty sets
   */
    public class ExtraHeuristicsDto {
        /**
     * List of extra heuristic values we want to optimize
     */
        public IList<HeuristicEntryDto> Heuristics { get; set; } = new List<HeuristicEntryDto>();

        public ExecutionDto DatabaseExecutionDto { get; set; }
    }
}