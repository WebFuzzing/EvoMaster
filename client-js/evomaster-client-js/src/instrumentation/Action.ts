export default class Action {

    private readonly index: number;

    /**
     * A list (possibly empty) of String values used in the action.
     * This info can be used for different kinds of taint analysis, eg
     * to check how such values are used in the SUT
     */
    private readonly inputVariables: Set<String>;

    constructor(index: number, inputVariables: Set<String>) {
        this.index = index;
        this.inputVariables = new Set<String>(inputVariables);
    }

    getIndex(): number {
        return this.index;
    }

    getInputVariables(): Set<String> {
        //TODO consider to use Immutable.js or equivalent
        return new Set<String>(this.inputVariables);
    }
}
