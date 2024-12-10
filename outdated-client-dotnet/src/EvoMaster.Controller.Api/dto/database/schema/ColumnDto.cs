namespace EvoMaster.Controller.Api {
    public class ColumnDto {
        public string Table { get; set; }

        public string Name { get; set; }

        public string Type { get; set; }

        public int Size { get; set; }

        public bool PrimaryKey { get; set; }

        public bool Nullable { get; set; }

        public bool Unique { get; set; }

        public bool AutoIncrement { get; set; }

        public bool ForeignKeyToAutoIncrement { get; set; }
    }
}