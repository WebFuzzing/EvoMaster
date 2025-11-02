/**
 * The type of extra heuristic.
 * Note: for the moment, we only have heuristics on SQL commands
 */
export enum Type {SQL = "SQL"}

/**
 * Should we try to minimize or maximize the heuristic?
 */
export enum Objective {
    /**
     * The lower the better.
     * Minimum is 0. It can be considered as a "distance" to minimize.
     */
    MINIMIZE_TO_ZERO = "MINIMIZE_TO_ZERO",
    /**
     * The higher the better.
     * Note: given x, we could rather considered the value
     * 1/x to minimize. But that wouldn't work for negative x,
     * and also would make debugging more difficult (ie better to
     * look at the raw, non-transformed values).
     */
    MAXIMIZE = "MAXIMIZE"
}

export class HeuristicEntryDto {

    public type: Type;

    public objective: Objective;

    /**
     * An id representing this heuristics.
     * For example, for SQL, it could be a SQL command
     */
    public id: string;

    /**
     * The actual value of the heuristic
     */
    public value: number;
}
