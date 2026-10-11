package org.evomaster.core.problem.asyncapi.param

import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsyncApiParamTest {

    @Test
    fun testACopyOfAnOpaquePayloadIsStillOpaque() {

        val payload = AsyncApiParam(AsyncApiParam.PAYLOAD, ObjectGene("p", listOf()), opaque = true)

        assertTrue((payload.copy() as AsyncApiParam).opaque)
    }

    @Test
    fun testAParamIsNotOpaqueUnlessSaidSo() {

        assertFalse(AsyncApiParam(AsyncApiParam.PAYLOAD, ObjectGene("p", listOf())).opaque)
    }

    @Test
    fun testOnlyAPayloadCanBeOpaque() {

        assertThrows(IllegalArgumentException::class.java) {
            AsyncApiParam(AsyncApiParam.HEADERS, ObjectGene("h", listOf()), opaque = true)
        }
    }
}
