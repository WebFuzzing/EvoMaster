package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

class ExperimentalFaultCategoryTest {

    @Test
    fun testUniqueCodes() {
        val total = DefinedFaultCategory.values().size + ExperimentalFaultCategory.values().size

        val unique = Stream.concat(
            Arrays.stream(DefinedFaultCategory.values()),
            Arrays.stream(ExperimentalFaultCategory.values())
        ).map { it.code }
            .collect(Collectors.toSet())
            .size

        assertEquals(total, unique, "Mismatch: $total != $unique")
    }
}