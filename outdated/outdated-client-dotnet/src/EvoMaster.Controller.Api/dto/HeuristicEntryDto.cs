namespace EvoMaster.Controller.Api {
    public class HeuristicEntryDto {
        public HeuristicEntryDto() { }

        public HeuristicEntryDto(Type type, Objective objective, string id, double value) {
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
        public double? Value { get; set; }
    }

    /**
   * Should we try to minimize or maximize the heuristic?
   */
    public enum Objective {
        /**
    * The lower the better.
    * Minimum is 0. It can be considered as a "distance" to minimize.
    */
        MINIMIZE_TO_ZERO,

        /**
    * The higher the better.
    * Note: given x, we could rather considered the value
    * 1/x to minimize. But that wouldn't work for negative x,
    * and also would make debugging more difficult (ie better to
    * look at the raw, non-transformed values).
    */
        MAXIMIZE
    }

    /**
   * The type of extra heuristic.
   * Note: for the moment, we only have heuristics on SQL commands
   */
    public enum Type {
        SQL
    }
}