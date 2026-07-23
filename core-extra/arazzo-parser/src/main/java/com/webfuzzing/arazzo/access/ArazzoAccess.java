package com.webfuzzing.arazzo.access;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Read Arrazzo documents
 */
public class ArazzoAccess {

    /**
     * Read Arrazzo documents from disk (files in the project)
     */
    public static String readFromDisk(String arazzoLocation) {
        String fileScheme = "file:";

        Path path;
        try {
            if (arazzoLocation.toLowerCase().startsWith(fileScheme)) {
                path = Paths.get(URI.create(arazzoLocation));
            } else {
                path = Paths.get(arazzoLocation);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("The file path provided for the Arazzo Schema " + arazzoLocation + " ended up with the following error: " + e.getMessage());
        }

        if ((!Files.exists(path))) {
            throw new IllegalArgumentException("The provided Arazzo file does not exist: " + arazzoLocation);
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error reading the Arazzo file: " + e.getMessage());
        }

    }
}
