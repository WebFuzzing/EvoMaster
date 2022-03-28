namespace EvoMaster.Controller.Api {
    public class HeaderDto {
        public HeaderDto() { }

        public HeaderDto(string name, string value) {
            this.Name = name;

            this.Value = value;
        }

        /**
     * The header name
     */
        public string Name { get; set; }

        /**
     * The value of the header
     */
        public string Value { get; set; }
    }
}