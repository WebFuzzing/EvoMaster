namespace EvoMaster.Instrumentation_Shared.Exceptions {
    public class CSharpDataTypeNotInferredException : InstrumentationException {
        public CSharpDataTypeNotInferredException(string typeName) : base(
            $"Unable to detect the c# data type for {typeName}") {
        }
    }
}