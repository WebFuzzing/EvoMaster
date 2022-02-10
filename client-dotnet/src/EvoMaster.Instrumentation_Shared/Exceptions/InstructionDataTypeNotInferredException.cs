using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation_Shared.Exceptions {
    public class InstructionDataTypeNotInferredException : InstrumentationException {
        public InstructionDataTypeNotInferredException(Instruction instruction) : base(
            $"Unable to detect the associated data type for instruction: {instruction}") {
        }
    }
}