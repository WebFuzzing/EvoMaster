package org.evomaster.client.java.instrumentation.example.positiveinteger;

import com.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;

public class PInotInstrumentedTest extends PositiveIntegerTestBase {

    @Override
    protected PositiveInteger getInstance() throws Exception {
        return new PositiveIntegerImp();
    }
}
