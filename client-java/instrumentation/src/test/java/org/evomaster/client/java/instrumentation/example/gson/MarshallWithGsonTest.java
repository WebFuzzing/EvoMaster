package org.evomaster.client.java.instrumentation.example.gson;

import com.foo.somedifferentpackage.examples.gson.MarshallWithGsonImp;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MarshallWithGsonTest {


    protected MarshallWithGson getInstance() throws Exception {

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0");

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (MarshallWithGson) cl.loadClass(MarshallWithGsonImp.class.getName()).newInstance();
    }


    @Test
    public void testFromJson() throws Exception{

        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();

        MarshallWithGson sut = getInstance();

        Object obj = sut.doMarshall("{\"foo\":\"hello\", \"bar\":42}");
        assertNotNull(obj);
        FooBar fooBar = (FooBar) obj;
        assertEquals("hello", fooBar.foo);
        assertEquals(42, fooBar.bar);

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<String> names = info.getParsedDtoNamesView();
        assertEquals(1, names.size());
        String name = names.iterator().next();
        assertEquals(FooBar.class.getName(), name);

        String schema = UnitsInfoRecorder.getInstance().getParsedDtos().get(name);
        assertNotNull(schema);
        assertTrue(schema.contains("foo"));
        assertTrue(schema.contains("bar"));
        assertFalse(schema.contains("hello")); //value of instance, not schema
        assertFalse(schema.contains("42"));
    }
}
