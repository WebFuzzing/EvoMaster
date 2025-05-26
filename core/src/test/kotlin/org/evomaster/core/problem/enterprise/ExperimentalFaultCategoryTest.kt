package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.util.stream.Collectors

class ExperimentalFaultCategoryTest{

  @Test
  fun testUniqueCodes() {
    val total = DefinedFaultCategory.values().size
   val unique = Arrays.stream(DefinedFaultCategory.values())
             .map { c: DefinedFaultCategory -> c.code }
            .collect(Collectors.toSet())
             .size

  assertEquals(total, unique, "Mismatch: $total != $unique")
  }
 }