
export default class SutRunDto {

    /**
     * Whether the SUT should be running
     */
    run: boolean;

    /**
     * Whether the internal state of the SUT should be reset
     */
    resetState: boolean;

    /**
     *  Whether SQL heuristics should be computed.
     *  Note: those can be very expensive
     */
    calculateSqlHeuristics: boolean;

    /**
     *  Whether SQL execution info should be saved.
     */
    extractSqlExecutionInfo: boolean;

}
