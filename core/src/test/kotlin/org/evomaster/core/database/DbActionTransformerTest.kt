package org.evomaster.core.database

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DbActionTransformerTest {

    @Test
    fun testEmpty() {
        val actions = listOf<DbAction>()
        val dto = DbActionTransformer.transform(actions)
        assertTrue(dto.insertions.isEmpty())
    }


}