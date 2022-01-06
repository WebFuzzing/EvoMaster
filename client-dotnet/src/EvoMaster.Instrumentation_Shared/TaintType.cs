namespace EvoMaster.Instrumentation_Shared {
    public enum TaintType {
        NONE,

        FULL_MATCH,

        PARTIAL_MATCH
    }

    public static class TaintTypeExtensions {
        public static bool IsTainted(this TaintType taintType) => taintType != TaintType.NONE;

        public static bool IsFullMatch(this TaintType taintType) => taintType == TaintType.FULL_MATCH;

        public static bool IsPartialMatch(this TaintType taintType) => taintType == TaintType.PARTIAL_MATCH;
    }
}