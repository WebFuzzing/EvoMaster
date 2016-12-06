package org.evomaster.clientJava.instrumentation;


import org.objectweb.asm.ClassReader;

import java.util.Objects;

public class Instrumentator {

    /**
     * Get the raw bytes of instrumented class with name {@code className}
     *
     * @param classLoader
     * @param className
     * @param reader
     * @return
     */
    public byte[] transformBytes(ClassLoader classLoader, String className, ClassReader reader) {
        Objects.requireNonNull(classLoader);
        Objects.requireNonNull(className);
        Objects.requireNonNull(reader);

        return null;//TODO
    }
}