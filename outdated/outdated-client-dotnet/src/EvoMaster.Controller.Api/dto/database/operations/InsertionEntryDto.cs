namespace EvoMaster.Controller.Api {
    public class InsertionEntryDto {
        public string VariableName { get; set; }

        public string PrintableValue { get; set; }

        /**
         * If non null, then printableValue should be null.
         * This should be an id of an InsertionDto previously
         * executed
         */
        public long? ForeignKeyToPreviouslyGeneratedRow { get; set; }
    }
}