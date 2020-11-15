using System.Collections.Generic;

namespace Controller.Api {
  public class ForeignKeyDto {

    public IList<string> SourceColumns = new List<string> ();

    public string TargetTable { get; set; }
  }
}