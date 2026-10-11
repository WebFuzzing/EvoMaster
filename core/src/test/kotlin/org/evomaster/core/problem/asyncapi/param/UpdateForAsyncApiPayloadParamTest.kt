package org.evomaster.core.problem.asyncapi.param

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UpdateForAsyncApiPayloadParamTest {

    companion object {
        private const val DTO = "com.foo.ReturnRequested"

        private const val FIELD = "reason"
    }

    private fun dto() = ObjectGene(DTO, listOf(StringGene(FIELD)))

    @Test
    fun testItReplacesThePayloadAndNothingElse() {

        val update = UpdateForAsyncApiPayloadParam(dto())

        assertTrue(update.isSameTypeWithUpdatedParam(AsyncApiParam(AsyncApiParam.PAYLOAD, ObjectGene("p", listOf()))))
        assertFalse(update.isSameTypeWithUpdatedParam(AsyncApiParam(AsyncApiParam.HEADERS, ObjectGene("h", listOf()))))
        assertFalse(update.isSameTypeWithUpdatedParam(UpdateForAsyncApiPayloadParam(dto())))
    }

    @Test
    fun testTheReplacementIsAPayloadThatIsNoLongerOpaque() {

        val replacement = UpdateForAsyncApiPayloadParam(dto()).getUpdatedParam()

        assertTrue(replacement is AsyncApiParam)
        assertEquals(AsyncApiParam.PAYLOAD, replacement.name)
        //otherwise every evaluation would line up another replacement for it
        assertFalse((replacement as AsyncApiParam).opaque)
        assertEquals(listOf(FIELD), (replacement.gene as ObjectGene).fields.map { it.name })
    }

    @Test
    fun testEachReplacementIsAGeneOfItsOwn() {

        val update = UpdateForAsyncApiPayloadParam(dto())

        //swapped into one copy of an individual, it must not be shared with another
        assertNotSame(update.getUpdatedParam().gene, update.getUpdatedParam().gene)
        assertNotSame(update.gene, (update.copy() as UpdateForAsyncApiPayloadParam).gene)
    }

    @Test
    fun testItIsNeverMistakenForThePayload() {

        assertNotEquals(AsyncApiParam.PAYLOAD, UpdateForAsyncApiPayloadParam(dto()).name)
    }
}
