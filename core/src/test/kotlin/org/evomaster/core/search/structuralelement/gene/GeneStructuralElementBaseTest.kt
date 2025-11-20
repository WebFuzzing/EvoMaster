package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

abstract class GeneStructuralElementBaseTest : StructuralElementBaseTest() {

    companion object{
        val randomness = Randomness()
    }

    abstract fun getCopyFromTemplate() : Gene

    abstract fun assertCopyFrom(base: Gene)

    open fun expectedChildrenSizeAfterRandomness() = getExpectedChildrenSize()

    open fun doForceNewValueInRandomness() : Boolean = true

    open fun additionalAssertionsAfterRandomness(base: Gene){}

    open fun throwExceptionInCopyFromTest() = false

    open fun throwExceptionInRandomnessTest() = false

    @Test
    fun testCopyFrom(){
        if (throwExceptionInCopyFromTest()) return

        val base = getStructuralElement()
        assertTrue(base is Gene)
        val template = getCopyFromTemplate()
        (base as Gene)

        if (throwExceptionInCopyFromTest()){
            assertThrows<Exception> {
                base.copyValueFrom(template)
            }
        }else{
            base.copyValueFrom(template)
            assertCopyFrom(base)
            // keep structure after copyfrom
            assertChildren(base, -1)
        }
    }

    @Test
    fun testAfterRandomness(){

        val base = getStructuralElement()
        assertTrue(base is Gene)
        if (base is Gene){
            if (throwExceptionInRandomnessTest())
                assertThrows<Exception> { base.randomize(randomness, true) }
            else{
                base.randomize(randomness, true)
                assertChildren(base, expectedChildrenSizeAfterRandomness())
                additionalAssertionsAfterRandomness(base)
            }
        }
    }

}