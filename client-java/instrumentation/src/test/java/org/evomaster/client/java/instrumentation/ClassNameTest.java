package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ClassNameTest {

    @Test
    public void checkLoadClass() throws IOException {

        ClassName className = new ClassName(this.getClass().getName());

        InputStream is =
                this.getClass().getClassLoader().getResourceAsStream(
                        className.getAsResourcePath()
                );

        assertNotNull(is);
        is.close();
    }

}