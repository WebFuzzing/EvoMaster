
export default class SutRunDto {

    /**
     * Whether the SUT should be running
     */
    public run: boolean;

    /**
     * Whether the internal state of the SUT should be reset
     */
    public resetState: boolean;

    /**
     *  Whether SQL heuristics should be computed.
     *  Note: those can be very expensive
     */
    public calculateSqlHeuristics: boolean;

    /**
     *  Whether SQL execution info should be saved.
     */
    public extractSqlExecutionInfo: boolean;

}
