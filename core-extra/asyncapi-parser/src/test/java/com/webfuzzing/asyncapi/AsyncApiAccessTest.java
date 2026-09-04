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

    @Test
    public void testAnotherDocumentNextToThisOneIsFollowed(@TempDir Path dir) throws IOException {

        write(dir.resolve("shared.yaml"),
                "components:\n"
                        + "  schemas:\n"
                        + "    Thing:\n"
                        + "      type: object\n"
                        + "      properties:\n"
                        + "        id:\n"
                        + "          type: string\n");

        Path main = write(dir.resolve("main.yaml"),
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Split across files\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    m:\n"
                        + "      payload:\n"
                        + "        $ref: 'shared.yaml#/components/schemas/Thing'\n");

        AsyncApiDocument document = AsyncApiAccess.getAsyncApiFromLocation(main.toString());

        assertTrue(document.getWarnings().isEmpty(), "unexpected warnings: " + document.getWarnings());
        assertEquals(1, document.getComponentSchemas().size());
        assertTrue(document.getComponentSchemas().keySet().iterator().next().startsWith("_ext_"));
        assertTrue(document.getMessages().containsKey("m"));
    }

    @Test
    public void testAnotherDocumentIsFollowedFromAFolderWhoseNameHasASpace(@TempDir Path dir)
            throws IOException {

        /*
            A space is not legal in a URI, and resolving a relative reference used to go through
            java.net.URI. That made following a reference depend on where the project happened
            to sit on disk: every external reference was dropped, with a warning saying the file
            did not exist, for any path containing a space -- and likewise on Windows, whose
            paths have backslashes and a drive letter.
         */
        Path folder = Files.createDirectories(dir.resolve("with space"));

        write(folder.resolve("shared.yaml"),
                "components:\n"
                        + "  schemas:\n"
                        + "    Thing:\n"
                        + "      type: string\n");

        Path main = write(folder.resolve("main.yaml"),
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: In a folder with a space in its name\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    m:\n"
                        + "      payload:\n"
                        + "        $ref: 'shared.yaml#/components/schemas/Thing'\n");

        AsyncApiDocument document = AsyncApiAccess.getAsyncApiFromLocation(main.toString());

        assertTrue(document.getWarnings().isEmpty(), "unexpected warnings: " + document.getWarnings());
        assertEquals(1, document.getComponentSchemas().size());
        assertTrue(document.getMessages().containsKey("m"));
    }

    @Test
    public void testTheSameLocationAlwaysGetsTheSameImportedNames(@TempDir Path dir) throws IOException {

        //the names imported components get have to be stable, or generated output would churn
        write(dir.resolve("shared.yaml"), "components:\n  schemas:\n    Thing:\n      type: string\n");

        Path main = write(dir.resolve("main.yaml"),
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Split\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    m:\n"
                        + "      payload:\n"
                        + "        $ref: 'shared.yaml#/components/schemas/Thing'\n");

        assertEquals(
                AsyncApiAccess.getAsyncApiFromLocation(main.toString()).getComponentSchemas().keySet(),
                AsyncApiAccess.getAsyncApiFromLocation(main.toString()).getComponentSchemas().keySet());
    }
}
