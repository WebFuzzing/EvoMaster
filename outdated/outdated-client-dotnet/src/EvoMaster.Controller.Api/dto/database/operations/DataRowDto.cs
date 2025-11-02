using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class DataRowDto {
        public IList<string> ColumnData { get; set; } = new List<string>();
    }
}