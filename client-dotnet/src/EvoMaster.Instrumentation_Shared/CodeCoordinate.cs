using System;

namespace EvoMaster.Instrumentation_Shared {
    public class CodeCoordinate {
        
        public CodeCoordinate(int line, int column) {
            Line = line;
            Column = column;
        }
        
        public int Line { get; }
        public int Column { get; }
    }
}