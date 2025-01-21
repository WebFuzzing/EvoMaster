package org.evomaster.core.solver;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.Assert.*;

public class FileTest {

    private Path tempDir;
    private Path testFilePath;

    @Before
    public void setUp() throws IOException {
        // Create a temporary directory for tests
        tempDir = Files.createTempDirectory("test-files");
        testFilePath = tempDir.resolve("example.txt");
    }

    @After
    public void tearDown() throws IOException {
        // Clean up after tests
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        if (Files.exists(tempDir)) {
            Files.delete(tempDir);
        }
    }

    @Test
    public void testWriteWithFileWriter() throws IOException {
        FileWriter writer = new FileWriter(testFilePath.toFile());
        writer.write("Hello, FileWriter!");
        writer.close();

        String content = new String(Files.readAllBytes(testFilePath), StandardCharsets.UTF_8);
        assertEquals("Hello, FileWriter!", content);
    }

    @Test
    public void testWriteWithBufferedWriter() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(testFilePath.toFile()));
        writer.write("Hello, BufferedWriter!");
        writer.close();

        String content = new String(Files.readAllBytes(testFilePath), StandardCharsets.UTF_8);
        assertEquals("Hello, BufferedWriter!", content);
    }

    @Test
    public void testWriteWithPrintWriter() throws IOException {
        PrintWriter writer = new PrintWriter(testFilePath.toFile());
        writer.println("Hello, PrintWriter!");
        writer.close();

        String content = new String(Files.readAllBytes(testFilePath), StandardCharsets.UTF_8).trim();
        assertEquals("Hello, PrintWriter!", content);
    }

    @Test
    public void testWriteWithFilesWriteMethod() throws IOException {
        String content = "Hello, Files.write!";
        Files.write(testFilePath, content.getBytes(StandardCharsets.UTF_8));

        String fileContent = new String(Files.readAllBytes(testFilePath), StandardCharsets.UTF_8);
        assertEquals(content, fileContent);
    }

    @Test
    public void testWriteWithOutputStream() throws IOException {
        FileOutputStream outputStream = new FileOutputStream(testFilePath.toFile());
        outputStream.write("Hello, OutputStream!".getBytes(StandardCharsets.UTF_8));
        outputStream.close();

        String content = new String(Files.readAllBytes(testFilePath), StandardCharsets.UTF_8);
        assertEquals("Hello, OutputStream!", content);
    }

    @Test
    public void testAppendToFile() throws IOException {
        // Write initial content
        Files.write(testFilePath, "Initial content\n".getBytes(StandardCharsets.UTF_8));

        // Append content
        FileWriter writer = new FileWriter(testFilePath.toFile(), true);
        writer.write("Appended content");
        writer.close();

        List<String> lines = Files.readAllLines(testFilePath, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("Initial content", lines.get(0));
        assertEquals("Appended content", lines.get(1));
    }
}
