using System;

namespace EvoMaster.Instrumentation_Shared {
    public class CodeCoordinate {
        
        public CodeCoordinate(int line, int column) {
            Line = line;
            Column = column;
        }

        private int Line { get; }
        private int Column { get; }

        public override bool Equals(object obj) {
            
            if (obj == null)
            {
                return false;
            }
            if (!(obj is CodeCoordinate coordinate))
            {
                return false;
            }
            return (Line == coordinate.Line)
                   && (Column == coordinate.Column);
        }

        protected bool Equals(CodeCoordinate other) {
            return Line == other.Line && Column == other.Column;
        }

        public override int GetHashCode() {
            return HashCode.Combine(Line, Column);
        }
    }
}