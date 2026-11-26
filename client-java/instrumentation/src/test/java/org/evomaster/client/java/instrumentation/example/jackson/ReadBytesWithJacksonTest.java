package org.evomaster.client.java.instrumentation.example.jackson;

import com.foo.somedifferentpackage.examples.jackson.ReadBytesWithJacksonImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Jackson replacements for reads from bytes, applied by the instrumentation to the SUT code and,
 * for spring-kafka's JsonDeserializer, to library code.
 */
public class ReadBytesWithJacksonTest {

    private static final String JSON = "{\"foo\":\"hello\", \"baz\":87878}";

    private static final int TAINT_ID = 42;

    /**
     * One way to read bytes with Jackson
     */
    @FunctionalInterface
    private interface Read {
        Object apply(ReadBytesWithJackson sut, byte[] json) throws Exception;
    }

    @BeforeEach
    public void initTest() {
        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();
    }

    private static ReadBytesWithJackson getInstance() throws Exception {

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,EXT_0");

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (ReadBytesWithJackson) cl.loadClass(ReadBytesWithJacksonImpl.class.getName()).newInstance();
    }

    private static void checkDtoIsParsed(Read read) throws Exception {
        ReadBytesWithJackson sut = getInstance();

        Object obj = read.apply(sut, JSON.getBytes(StandardCharsets.UTF_8));
        FooBaz fooBaz = (FooBaz) obj;
        assertEquals("hello", fooBaz.foo);
        assertEquals(87878, fooBaz.baz);

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<String> names = info.getParsedDtoNamesView();
        assertEquals(1, names.size(), "Parsed DTOs: " + names);
        assertEquals(FooBaz.class.getName(), names.iterator().next());

        String schema = UnitsInfoRecorder.getInstance().getParsedDtos().get(FooBaz.class.getName());
        assertNotNull(schema);
        assertTrue(schema.contains("foo"));
        assertTrue(schema.contains("baz"));
        assertTrue(info.getStringSpecializationsView().isEmpty(), "A document that is not a taint must not be specialized");
    }

    private static void checkTaintIsSpecialized(Read read) throws Exception {
        ReadBytesWithJackson sut = getInstance();
        String taint = TaintInputName.getTaintName(TAINT_ID);

        // a taint is not valid JSON, so the read fails after the analysis
        assertThrows(Exception.class, () -> read.apply(sut, taint.getBytes(StandardCharsets.UTF_8)));

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        Set<StringSpecializationInfo> ss = info.getStringSpecializationsView().get(taint);
        assertNotNull(ss, "No string specialization for the taint");
        assertEquals(1, ss.size());
        StringSpecializationInfo ssi = ss.iterator().next();
        assertEquals(StringSpecialization.JSON_OBJECT, ssi.getStringSpecialization());
        assertTrue(ssi.getValue().contains(FooBaz.class.getSimpleName()), "Invalid type: " + ssi.getValue());
    }

    @Test
    public void testObjectReader() throws Exception {
        checkDtoIsParsed(ReadBytesWithJackson::readWithObjectReader);
    }

    @Test
    public void testObjectReaderTaint() throws Exception {
        checkTaintIsSpecialized(ReadBytesWithJackson::readWithObjectReader);
    }

    @Test
    public void testObjectMapper() throws Exception {
        checkDtoIsParsed(ReadBytesWithJackson::readWithObjectMapper);
    }

    @Test
    public void testObjectMapperTaint() throws Exception {
        checkTaintIsSpecialized(ReadBytesWithJackson::readWithObjectMapper);
    }

    @Test
    public void testSpringKafkaJsonDeserializer() throws Exception {
        /*
            The SUT calls no Jackson method itself, so FooBaz can only be registered by the replacement
            applied inside JsonDeserializer.deserialize
         */
        checkDtoIsParsed(ReadBytesWithJackson::readWithSpringKafkaJsonDeserializer);
    }

    @Test
    public void testSpringKafkaJsonDeserializerTaint() throws Exception {
        checkTaintIsSpecialized(ReadBytesWithJackson::readWithSpringKafkaJsonDeserializer);
    }
}
