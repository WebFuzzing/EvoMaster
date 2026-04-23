using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class TestResultsDto {
        public IList<TargetInfoDto> Targets { get; set; } = new List<TargetInfoDto>();

        /**
     * This list is sorted based on the action indices
     */
        public IList<AdditionalInfoDto> AdditionalInfoList { get; set; } = new List<AdditionalInfoDto>();

        public IList<ExtraHeuristicsDto> ExtraHeuristics { get; set; } = new List<ExtraHeuristicsDto>();
    }
}