package org.evomaster.client.java.instrumentation.example.jackson;

import com.foo.somedifferentpackage.examples.jackson.ReadWithJacksonImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ReadWithJacksonTest {

    protected ReadWithJackson getInstance() throws Exception {

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "EXT_0,SQL");

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (ReadWithJackson) cl.loadClass(ReadWithJacksonImpl.class.getName()).newInstance();
    }

    @Test
    public void testReadFromString() throws Exception{

        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();

        ReadWithJackson sut = getInstance();

        Object obj = sut.readValue("{\"foo\":\"hello\", \"baz\":87878}");
        assertNotNull(obj);
        FooBaz fooBar = (FooBaz) obj;
        assertEquals("hello", fooBar.foo);
        assertEquals(87878, fooBar.baz);

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<String> names = info.getParsedDtoNamesView();
        assertEquals(1, names.size());
        String name = names.iterator().next();
        assertEquals(FooBaz.class.getName(), name);

        String schema = UnitsInfoRecorder.getInstance().getParsedDtos().get(name);
        assertNotNull(schema);
        assertTrue(schema.contains("foo"));
        assertTrue(schema.contains("baz"));
        assertFalse(schema.contains("hello")); //value of instance, not schema
        assertFalse(schema.contains("87878"));
    }
}
