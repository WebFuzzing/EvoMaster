package org.evomaster.client.java.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringsExampleImp;

public class SEnotInstrumentedTest extends StringsExampleTestBase{

    @Override
    protected StringsExample getInstance() {
        return new StringsExampleImp();
    }
}
