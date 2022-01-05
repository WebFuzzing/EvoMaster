namespace EvoMaster.Instrumentation_Shared {
    public class CodeCoordinate {
        
        public CodeCoordinate(int line, int column) {
            Line = line;
            Column = column;
        }
        
        public int Line { get; set; }
        public int Column { get; set; }

        public override bool Equals(object obj) {
            var casted = (CodeCoordinate) obj;
            return Equals(casted);
        }

        private bool Equals(CodeCoordinate other) {
            return Line == other.Line && Column == other.Column;
        }

        private static CodeCoordinate Cast(object obj) {
            return (CodeCoordinate) obj;
        }
    }
}