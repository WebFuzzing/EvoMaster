package com.webfuzzing.asyncapi;

import com.webfuzzing.asyncapi.access.AsyncApiAccess;
import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.parser.AsyncApiParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * How a document is retrieved, as opposed to what is made of it once it has been.
 *
 * The classpath route is what every other test uses, so what is worth covering here is the one a
 * user actually takes -- a path on disk -- and the errors they will meet when it is wrong.
 */
public class AsyncApiAccessTest {

    private static final String MINIMAL =
            "asyncapi: 3.0.0\n"
                    + "info:\n"
                    + "  title: Minimal\n"
                    + "  version: 1.0.0\n"
                    + "components:\n"
                    + "  messages:\n"
                    + "    m:\n"
                    + "      payload:\n"
                    + "        type: object\n";

    private static Path write(Path path, String content) throws IOException {
        return Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testLoadingFromAFilePath(@TempDir Path dir) throws IOException {

        Path file = write(dir.resolve("asyncapi.yaml"), MINIMAL);

        AsyncApiDocument document = AsyncApiAccess.getAsyncApiFromLocation(file.toString());

        assertEquals(new LinkedHashSet<>(Arrays.asList("m")), document.getMessages().keySet());
        assertEquals(file.toString(), document.getSourceLocation().getLocation());
        assertEquals(MINIMAL, document.getRawText());
    }

    @Test
    public void testLoadingFromAFileUrl(@TempDir Path dir) throws IOException {

        Path file = write(dir.resolve("asyncapi.yaml"), MINIMAL);

        AsyncApiDocument document = AsyncApiAccess.getAsyncApiFromLocation(file.toUri().toString());

        assertEquals(new LinkedHashSet<>(Arrays.asList("m")), document.getMessages().keySet());
    }

    @Test
    public void testAFileThatIsNotThere(@TempDir Path dir) {

        AsyncApiParsingException e = assertThrows(
                AsyncApiParsingException.class,
                () -> AsyncApiAccess.getAsyncApiFromLocation(dir.resolve("absent.yaml").toString()));

        assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
    }

    @Test
    public void testAResourceThatIsNotThere() {

        AsyncApiParsingException e = assertThrows(
                AsyncApiParsingException.class,
                () -> AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/absent.yaml"));

        assertTrue(e.getMessage().contains("classpath"), e.getMessage());
    }

}
