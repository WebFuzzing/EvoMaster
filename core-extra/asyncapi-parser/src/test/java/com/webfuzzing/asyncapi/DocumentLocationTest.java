package com.webfuzzing.asyncapi;

import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.models.DocumentLocationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentLocationTest {

    @Test
    public void testEqualityIsByLocationAndType() {

        DocumentLocation a = DocumentLocation.ofLocal("/a/main.yaml");
        DocumentLocation same = new DocumentLocation("/a/main.yaml", DocumentLocationType.LOCAL);

        assertEquals(a, a);
        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());

        //the same text read a different way is a different location
        assertNotEquals(a, new DocumentLocation("/a/main.yaml", DocumentLocationType.RESOURCE));
        assertNotEquals(a, DocumentLocation.ofLocal("/b/main.yaml"));

        assertNotEquals(a, null);
        assertNotEquals(a, "/a/main.yaml");
    }

    @Test
    public void testToStringSaysWhatAndWhere() {

        String text = DocumentLocation.ofRemote("https://example.com/main.yaml").toString();

        assertTrue(text.contains("REMOTE"), text);
        assertTrue(text.contains("https://example.com/main.yaml"), text);
    }

    @Test
    public void testAPlainPathIsOnlyALocalLocationWithoutAScheme() {

        assertTrue(DocumentLocation.ofLocal("/a/main.yaml").isPlainFilePath());
        assertTrue(DocumentLocation.ofLocal("C:\\dir\\main.yaml").isPlainFilePath());

        assertFalse(DocumentLocation.ofLocal("file:///a/main.yaml").isPlainFilePath());
        assertFalse(DocumentLocation.ofLocal("FILE:///a/main.yaml").isPlainFilePath());
        assertFalse(DocumentLocation.ofRemote("https://example.com/main.yaml").isPlainFilePath());
        assertFalse(DocumentLocation.ofResource("/asyncapi/main.yaml").isPlainFilePath());
        assertFalse(DocumentLocation.MEMORY.isPlainFilePath());
    }
}
