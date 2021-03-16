using System;

namespace EvoMaster.Instrumentation
{
    [Serializable]
    public class TargetInfo
    {
        public readonly int _mappedId;

        public readonly string _descriptiveId;

        /**
        * heuristic [0,1], where 1 means covered
        */
        public readonly double _value;

        /**
         * Can be negative if target was never reached.
         * But this means that {@code value} must be 0
         */
        public readonly int _actionIndex;
    }
}