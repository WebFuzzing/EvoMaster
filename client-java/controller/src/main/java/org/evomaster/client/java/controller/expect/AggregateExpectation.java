package org.evomaster.client.java.controller.expect;

public interface AggregateExpectation {

    //IndividualExpectation expect(Boolean active, Boolean condition);

    IndividualExpectation expect();

    /*default IndividualExpectation expect(Boolean condition){
        return expect(false, condition);
    }*/
}
