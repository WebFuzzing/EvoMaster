package org.evomaster.clientJava.instrumentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class InstrumentingClassLoaderTest {

    @Test
    public void checkLoadClass() throws IOException {

        InputStream is =
                this.getClass().getClassLoader().getResourceAsStream(
                        InstrumentingClassLoaderTest.class.getName()
                        .replace(".","/")
                        + ".class"
                );

        assertNotNull(is);
        is.close();
    }
}