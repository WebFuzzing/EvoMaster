package org.evomaster.client.java.controller.api.dto;

public class SutRunDto {

    /**
     * Whether the SUT should be running
     */
    public Boolean run;

    /**
     * Whether the internal state of the SUT should be reset
     */
    public Boolean resetState;

    /**
     *  Whether SQL heuristics should be computed.
     *  Note: those can be very expensive
     */
    public Boolean calculateSqlHeuristics;

    public SutRunDto() {
    }

    public SutRunDto(Boolean run, Boolean resetState, Boolean calculateSqlHeuristics) {
        this.run = run;
        this.resetState = resetState;
        this.calculateSqlHeuristics = calculateSqlHeuristics;
    }
}
