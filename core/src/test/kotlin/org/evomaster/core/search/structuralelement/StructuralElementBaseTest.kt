package org.evomaster.core.search.structuralelement

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

abstract class StructuralElementBaseTest {

    abstract fun getStructuralElement() : StructuralElement

    abstract fun getExpectedChildrenSize() : Int

    fun getStructuralElementAndIdentifyAsRoot() : StructuralElement{
        return getStructuralElement()//.apply { identifyAsRoot() }
    }

    @Test
    fun testInstance(){
        val template = getStructuralElementAndIdentifyAsRoot()
        assertChildren(template, getExpectedChildrenSize())
    }

    @Test
    fun testCopy(){
        val template = getStructuralElementAndIdentifyAsRoot()
        val copy = template.copy()
        assertCopy(template, copy)
    }

    fun assertChildren(obj : StructuralElement, expectedSize: Int){
        obj.getViewOfChildren().apply {
            if (expectedSize!= -1)
                assertEquals(expectedSize, size)
            forEach {
                assertNotNull(it.parent)
                assertEquals(obj, it.parent)
                if (it.getViewOfChildren().isNotEmpty()){
                    assertChildren(it, -1)
                }
            }
        }
    }

    fun assertCopy(template: StructuralElement, copy: StructuralElement, expectedSize: Int?=null){

        //same type
        assertEquals(template::class.java.simpleName, copy::class.java.simpleName)

        //assert size of children and every children should have a parent
        val size = expectedSize?: template.getViewOfChildren().size
        assertChildren(template, size)
        assertChildren(copy, size)

        //same traverseBackIndex
        val templateIndex = mutableListOf<Int>()
        template.traverseBackIndex(templateIndex)
        val copyIndex = mutableListOf<Int>()
        copy.traverseBackIndex(copyIndex)
        assertEquals(templateIndex, copyIndex)

        (0 until size).forEach {
            assertCopy(template.getViewOfChildren()[it], copy.getViewOfChildren()[it])
        }
    }
}