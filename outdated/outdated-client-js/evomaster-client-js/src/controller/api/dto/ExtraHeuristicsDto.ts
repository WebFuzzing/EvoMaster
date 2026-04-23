import {HeuristicEntryDto} from "./HeuristicEntryDto";

/**
 * Represents possible extra heuristics related to the code
 * execution and that do apply to all the reached testing targets.
 *
 * Example: rewarding SQL "select" operations that return non-empty sets
 */
export default class ExtraHeuristicsDto {

    /**
     * List of extra heuristic values we want to optimize
     */
     public heuristics: HeuristicEntryDto[] = [];

     // TODO
    // databaseExecutionDto: ExecutionDto;
}
