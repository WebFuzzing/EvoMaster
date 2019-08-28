package org.evomaster.client.java.instrumentation.example.methodreplacement.strings;

import com.foo.somedifferentpackage.examples.methodreplacement.strings.StringCallsImp;

public class SCnotInstrumentedTest extends StringCallsTestBase {
    @Override
    protected StringCalls getInstance() throws Exception {
        return new StringCallsImp();
    }
}
