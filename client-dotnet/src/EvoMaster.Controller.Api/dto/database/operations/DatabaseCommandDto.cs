using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class DatabaseCommandDto {
        /**
         * A generic SQL command.
         * Must be null if "insertions" field != null
         */
        public string Command { get; set; }

        /**
         * One or more insertion operation via SQL.
         * Must be null if "command" field != null.
         * Ids must be unique, but no need to be in any
         * specific order.
         * However, an insertion X referring to Y should
         * come in this list AFTER Y.
         */
        public IList<InsertionDto> insertions { get; set; } = new List<InsertionDto>();
    }
}