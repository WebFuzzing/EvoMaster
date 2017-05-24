package org.evomaster.clientJava.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringCallsImp;

public class SCnotInstrumentedTest extends StringCallsTestBase {
    @Override
    protected StringCalls getInstance() throws Exception {
        return new StringCallsImp();
    }
}
