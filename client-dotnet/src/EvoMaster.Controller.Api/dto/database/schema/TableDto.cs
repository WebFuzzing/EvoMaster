using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class TableDto {
        /**
     * The name of the table
     */
        public string Name { get; set; }

        /**
     * A list of descriptions for each column in the table
     */
        public IList<ColumnDto> Columns { get; set; } = new List<ColumnDto>();

        /**
     * Constraints on the table for foreign keys, if any
     */
        public IList<ForeignKeyDto> ForeignKeys { get; set; } = new List<ForeignKeyDto>();

        /**
     * Order in which the columns in the primary keys are listed
     * in the schema.
     *
     * For example, the primary key (activity_1_id, activity_2_id) results
     * in the list "activity_1_id", "activity_2_id".
     *
     * If the primary key has only one column, this sequence has only one
     * element.
     */
        public IList<string> PrimaryKeySequence { get; set; } = new List<string>();

        /**
     * All constraints that are not directly supported
     */
        public IList<TableCheckExpressionDto> TableCheckExpressions { get; set; } = new List<TableCheckExpressionDto>();
    }
}