package com.webfuzzing.asyncapi.access;

import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.models.DocumentLocationType;
import com.webfuzzing.asyncapi.parser.AsyncApiParser;
import com.webfuzzing.asyncapi.parser.AsyncApiParsingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Reads AsyncAPI documents and hands them to {@link AsyncApiParser}.
 *
 * A document may come from a URL, from disk, or from the classpath, and where it came from is
 * remembered so that references to neighbouring documents can be followed.
 */
public class AsyncApiAccess {

    /*
        TODO This class will likely need to be refactored. Retrieving a schema document from a
        URL, a file or the classpath is common functionality rather than anything specific to
        AsyncAPI: it could live in EvoMaster, or in a shared third-party library reused by the
        different document parsers. It is kept here for now, as that architectural decision is
        not being made yet.

        Note that AsyncApiParser.parse takes an AsyncApiDocumentFetcher, so retrieval is already
        behind an interface: whichever way that decision goes, the parser itself is unaffected.
     */

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    private static final int READ_TIMEOUT_MS = 30_000;

    private AsyncApiAccess() {
    }

    /**
     * Retrieve and parse an AsyncAPI document. The location can be a remote http(s) URL, a
     * local file URL, or a plain file path.
     */
    public static AsyncApiDocument getAsyncApiFromLocation(String location) {

        DocumentLocationType type = location.toLowerCase(Locale.ENGLISH).startsWith("http")
                ? DocumentLocationType.REMOTE
                : DocumentLocationType.LOCAL;

        return parse(fetch(location, type), new DocumentLocation(location, type));
    }

    /**
     * Retrieve and parse an AsyncAPI document off the classpath.
     */
    public static AsyncApiDocument getAsyncApiFromResource(String location) {
        return parse(
                fetch(location, DocumentLocationType.RESOURCE),
                DocumentLocation.ofResource(location));
    }

    /**
     * Parse a document that is already in hand. References to other documents cannot be
     * followed from here, as there is no location to resolve them against.
     */
    public static AsyncApiDocument parseFromText(String schemaText) {
        return parse(schemaText, DocumentLocation.MEMORY);
    }

    /*
        Note that nothing is reported to the caller here. Everything the parser had to skip is
        on AsyncApiDocument.getWarnings(), and where those warnings go is the caller's decision.
     */
    private static AsyncApiDocument parse(String schemaText, DocumentLocation location) {
        return AsyncApiParser.parse(schemaText, location, AsyncApiAccess::fetch);
    }

    /**
     * Read the text of a document, wherever it lives. Also used to follow references to other
     * documents, which is why it takes the kind of location explicitly.
     */
    private static String fetch(String location, DocumentLocationType type) {

        switch (type) {
            case REMOTE:
                return readFromRemoteServer(location);
            case RESOURCE:
                return readFromResource(location);
            case LOCAL:
                return readFromDisk(location);
            default:
                throw new AsyncApiParsingException("There is no document to retrieve at '" + location + "'");
        }
    }

    /**
     * Read a document off the classpath.
     */
    public static String readFromResource(String location) {

        try (InputStream stream = AsyncApiAccess.class.getResourceAsStream(location)) {

            if (stream == null) {
                throw new AsyncApiParsingException(
                        "Cannot find the AsyncAPI document on the classpath: " + location);
            }

            return readAll(stream);

        } catch (IOException e) {
            throw new AsyncApiParsingException(
                    "Error reading the AsyncAPI document from the classpath: " + location, e);
        }
    }

    /**
     * Read a document from disk, given either as a path or as a {@code file:} URL.
     */
    public static String readFromDisk(String location) {

        String fileScheme = "file:";
        Path path;

        try {
            if (location.toLowerCase(Locale.ENGLISH).startsWith(fileScheme)) {
                path = Paths.get(URI.create(location));
            } else {
                path = Paths.get(location);
            }
        } catch (Exception e) {
            throw new AsyncApiParsingException(
                    "The file path provided for the AsyncAPI schema " + location
                            + " ended up with the following error: " + e.getMessage(), e);
        }

        if (!Files.exists(path)) {
            throw new AsyncApiParsingException("The provided AsyncAPI file does not exist: " + location);
        }

        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AsyncApiParsingException("Error reading the AsyncAPI file: " + e.getMessage(), e);
        }
    }

    /**
     * Read a document over http(s).
     *
     * Deliberately plain: no authentication, and no retrying while a server that is still
     * starting up refuses connections. A caller that needs either has more context to do it
     * with, and can fetch the text itself and use {@link #parseFromText(String)}.
     */
    public static String readFromRemoteServer(String url) {

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            //cannot assume it is in JSON... could be YAML as well
            connection.setRequestProperty("Accept", "*/*");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            int status = connection.getResponseCode();

            if (status < 200 || status >= 300) {
                throw new AsyncApiParsingException(
                        "Cannot retrieve the AsyncAPI schema from " + url + " , status=" + status);
            }

            try (InputStream stream = connection.getInputStream()) {
                return readAll(stream);
            }

        } catch (AsyncApiParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new AsyncApiParsingException("Failed to connect to " + url + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;

        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
