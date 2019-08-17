package org.evomaster.client.java.instrumentation.example.methodreplacement.strings;

import com.foo.somedifferentpackage.examples.methodreplacement.strings.StringsExampleImp;

public class SEnotInstrumentedTest extends StringsExampleTestBase{

    @Override
    protected StringsExample getInstance() {
        return new StringsExampleImp();
    }
}
