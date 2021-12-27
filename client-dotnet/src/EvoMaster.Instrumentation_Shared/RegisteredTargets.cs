using System.Collections.Generic;

namespace EvoMaster.Instrumentation_Shared {
    public class RegisteredTargets {
        public ICollection<string> Lines { get; set; } = new HashSet<string>();
        public ICollection<string> Classes { get; set; } = new HashSet<string>();
        public ICollection<string> Branches { get; set; } = new HashSet<string>();
    }
}