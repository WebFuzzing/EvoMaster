package org.evomaster.client.java.controller.expect;

public interface AggregateExpectation {

    //IndividualExpectation expect(Boolean active, Boolean condition);

    AggregateExpectation expect();

    AggregateExpectation expect(boolean masterSwitch);

    /*default IndividualExpectation expect(Boolean condition){
        return expect(false, condition);
    }*/
}
