package org.evomaster.clientJava.instrumentation.example.positiveinteger;

import org.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;

public class PInotInstrumentedTest extends PositiveIntegerTestBase {

    @Override
    protected PositiveInteger getInstance() throws Exception {
        return new PositiveIntegerImp();
    }
}
