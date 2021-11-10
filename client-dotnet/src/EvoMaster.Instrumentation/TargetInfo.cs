using System;

namespace EvoMaster.Instrumentation {
    [Serializable]
    public class TargetInfo {
        public readonly int? MappedId;

        public readonly string DescriptiveId;

        /**
        * heuristic [0,1], where 1 means covered
        */
        public readonly double? Value;

        /**
         * Can be negative if target was never reached.
         * But this means that {@code value} must be 0
         */
        public readonly int? ActionIndex;

        public TargetInfo(int? mappedId, string descriptiveId, double? value, int? actionIndex) {
            MappedId = mappedId;
            DescriptiveId = descriptiveId;
            Value = value;
            ActionIndex = actionIndex;
        }
    }
}