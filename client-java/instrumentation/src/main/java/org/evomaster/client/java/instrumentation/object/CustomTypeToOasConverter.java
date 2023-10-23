package org.evomaster.client.java.instrumentation.object;

public abstract class CustomTypeToOasConverter {
    public abstract String convert();
    public abstract boolean isInstanceOf(Class<?> klass);
}
