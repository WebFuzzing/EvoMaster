package org.evomaster.clientJava.controllerApi.dto;

public class SutRunDto {

    /**
     * Whether the SUT should be running
     */
    public Boolean run;

    /**
     * Whether the internal state of the SUT should be reset
     */
    public Boolean resetState;


    public SutRunDto() {
    }

    public SutRunDto(Boolean run, Boolean resetState) {
        this.run = run;
        this.resetState = resetState;
    }
}
