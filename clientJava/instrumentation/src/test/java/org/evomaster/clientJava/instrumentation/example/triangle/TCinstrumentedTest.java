package org.evomaster.clientJava.instrumentation.example.triangle;

import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;

public class TCinstrumentedTest extends TriangleClassificationTestBase {

    @Override
    protected TriangleClassification getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("org.foo");

        return (TriangleClassification)
                cl.loadClass(TriangleClassificationImpl.class.getName())
                .newInstance();
    }
}
