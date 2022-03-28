using System;
using System.Runtime.CompilerServices;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation_Shared {
    public static class SequencePointExtensions {
        public static bool HasEqualLengthWith(this SequencePoint first, SequencePoint second) =>
            first.HasEqualStartPointWith(second) && first.HasEqualFinishPointWith(second);

        private static bool HasEqualStartPointWith(this SequencePoint first, SequencePoint second) =>
            first.StartLine == second.StartLine && first.StartColumn == second.StartColumn;

        private static bool HasEqualFinishPointWith(this SequencePoint first, SequencePoint second) =>
            first.EndLine == second.EndLine && first.EndColumn == second.EndColumn;
    }
}