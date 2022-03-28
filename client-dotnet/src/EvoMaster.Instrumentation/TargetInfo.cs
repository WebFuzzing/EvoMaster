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

        public static TargetInfo NotReached(int theId) {
            return new TargetInfo(theId, null, 0d, -1);
        }

        public TargetInfo WithMappedId(int theId) {
            if (MappedId != null) {
                throw new ArgumentException("Id already existing");
            }

            return new TargetInfo(theId, DescriptiveId, Value, ActionIndex);
        }

        public TargetInfo WithNoDescriptiveId() {
            return new TargetInfo(MappedId, null, Value, ActionIndex);
        }
    }
}