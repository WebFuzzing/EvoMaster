using System;

namespace EvoMaster.Instrumentation_Shared.Exceptions {
    public class InstrumentationException : Exception {
        public InstrumentationException(string message) : base(message) {
        }
    }
}