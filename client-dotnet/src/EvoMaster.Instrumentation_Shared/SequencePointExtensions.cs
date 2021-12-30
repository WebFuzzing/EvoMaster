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

    public class CodeCoordination {
        public int Line { get; set; }
        public int Column { get; set; }

        public override bool Equals(object obj) {
            var casted = (CodeCoordination) obj;
            return Equals(casted);
        }

        private bool Equals(CodeCoordination other) {
            return Line == other.Line && Column == other.Column;
        }

        private static CodeCoordination Cast(object obj) {
            return (CodeCoordination) obj;
        }
    }
}