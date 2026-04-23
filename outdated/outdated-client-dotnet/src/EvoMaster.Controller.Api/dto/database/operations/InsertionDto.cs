using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class InsertionDto {
        /**
         * The ID of this insertion operation.
         * This is needed when we have multiple insertions, where
         * we need to refer (eg foreign key) to the data generated
         * by a previous insertion.
         * It can be null.
         */
        public long? Id { get; set; }

        public string TargetTable { get; set; }

        public IList<InsertionEntryDto> data { get; set; } = new List<InsertionEntryDto>();
    }
}