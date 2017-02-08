package org.evomaster.clientJava.instrumentation.example.triangle;

import com.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;

public class TCnotInstrumentedTest extends TriangleClassificationTestBase{

    @Override
    protected TriangleClassification getInstance() {
        return new TriangleClassificationImpl();
    }
}
