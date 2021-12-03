namespace EvoMaster.Controller.Api {
    using System.Collections.Generic;

    public class DbSchemaDto {
        public DatabaseType DatabaseType { get; set; }

        public string Name { get; set; }

        public IList<TableDto> Tables { get; set; } = new List<TableDto>();
    }
}