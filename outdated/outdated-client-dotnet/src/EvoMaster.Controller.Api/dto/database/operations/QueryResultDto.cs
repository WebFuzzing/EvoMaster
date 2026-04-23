using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class QueryResultDto {
        public IList<DataRowDto> Rows { get; set; } = new List<DataRowDto>();
    }
}