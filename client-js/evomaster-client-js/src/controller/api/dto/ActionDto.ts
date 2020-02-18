export default class ActionDto {

    /**
     * The index of this action in the test.
     * Eg, in a test with 10 indices, the index would be
     * between 0 and 9
     */
    public index: number;

    /**
     * A list (possibly empty) of String values used in the action.
     * This info can be used for different kinds of taint analysis, eg
     * to check how such values are used in the SUT
     */
    public inputVariables = new Array<string>();
}
