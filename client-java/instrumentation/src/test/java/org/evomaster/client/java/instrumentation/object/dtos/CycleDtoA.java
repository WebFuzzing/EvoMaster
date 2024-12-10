package org.evomaster.client.java.instrumentation.object.dtos;

public class CycleDtoA {

    private String cycleAId;

    private CycleDtoB cycleDtoB;

    public String getCycleAId() {
        return cycleAId;
    }

    public CycleDtoB getCycleDtoB() {
        return cycleDtoB;
    }
}
