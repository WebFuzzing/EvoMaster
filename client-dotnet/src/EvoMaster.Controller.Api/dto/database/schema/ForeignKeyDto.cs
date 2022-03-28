using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class ForeignKeyDto {
        public IList<string> SourceColumns { get; set; } = new List<string>();

        public string TargetTable { get; set; }
    }
}