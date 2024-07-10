namespace EvoMaster.Controller.Api {
    public class StringSpecializationInfoDto {
        public StringSpecializationInfoDto() { }

        public StringSpecializationInfoDto(string stringSpecialization, string value, string type) {
            this.StringSpecialization = stringSpecialization;
            this.Value = value;
            this.Type = type;
        }

        public string StringSpecialization { get; set; }

        public string Value { get; set; }

        public string Type { get; set; }
    }
}