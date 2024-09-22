package com.webfuzzing.commons.faults;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FaultCategoryTest {

    @Test
    public void testUniqueCodes() {

        int total = FaultCategory.values().length;
        int unique = Arrays.stream(FaultCategory.values())
                .map(c -> c.code)
                .collect(Collectors.toSet())
                .size();

        assertEquals(total, unique, "Mismatch: " + total + " != " + unique);
    }
}