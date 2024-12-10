package org.evomaster.client.java.instrumentation.object.dtos;

public class CycleDtoB {

    private String cycleBId;

    private CycleDtoA cycleDtoA;

    public CycleDtoA getCycleDtoA() {
        return cycleDtoA;
    }

    public String getCycleBId() {
        return cycleBId;
    }
}
