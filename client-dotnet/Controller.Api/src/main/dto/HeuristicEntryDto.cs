namespace Controller.Api {
  public class HeuristicEntryDto {

    public HeuristicEntryDto () { }

    public HeuristicEntryDto (Type type, Objective objective, string id, double value) {
      this.Type = type;
      this.Objective = objective;
      this.Id = id;
      this.Value = value;
    }

    public Type Type { get; set; }

    public Objective Objective { get; set; }

    /**
     * An id representing this heuristics.
     * For example, for SQL, it could be a SQL command
     */
    public string Id { get; set; }

    /**
     * The actual value of the heuristic
     */
    public double Value { get; set; }

  }
}