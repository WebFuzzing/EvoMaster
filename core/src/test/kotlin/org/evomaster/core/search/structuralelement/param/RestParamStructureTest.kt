package org.evomaster.core.search.structuralelement.param

import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BodyParamStructureTest : StructuralElementBaseTest() {

    override fun getStructuralElement(): BodyParam = BodyParam(
        ObjectGene(
            "obj",
            listOf(IntegerGene("f1"), DoubleGene("f2"), LongGene("f3"))),
        EnumGene("contentType", listOf("application/json"))
    )

    override fun getExpectedChildrenSize(): Int  = 3

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val f3 = (root.gene as ObjectGene).fields[2]
        assertEquals("f3", f3.name)

        val objectPath = mutableListOf<StructuralElement>()
        f3.traverseBack(objectPath)
        assertEquals(mutableListOf(root, root.gene, f3), objectPath)

        val path = mutableListOf<Int>()
        f3.traverseBackIndex(path)
        assertEquals(mutableListOf(0, 2), path)
    }
}

class HeaderParamStructureTest : StructuralElementBaseTest() {

    override fun getStructuralElement(): HeaderParam = HeaderParam("header", StringGene("key:value"))

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testChildType(){
        val update = getStructuralElement()
        assertTrue(update.getViewOfChildren().first() is Gene)
    }
}

class QueryParamStructureTest : StructuralElementBaseTest() {

    override fun getStructuralElement(): QueryParam = QueryParam("query", StringGene("query"))

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testChildType(){
        val update = getStructuralElement()
        assertTrue(update.getViewOfChildren().first() is Gene)
    }
}

class UpdateForBodyParamStructureTest : StructuralElementBaseTest() {

    override fun getStructuralElement(): UpdateForBodyParam = UpdateForBodyParam(BodyParam(
        ObjectGene(
            "obj",
            listOf(IntegerGene("f1"), DoubleGene("f2"), LongGene("f3"))),
        EnumGene("contentType", listOf("application/json"))
    ))

    override fun getExpectedChildrenSize(): Int = 3

    @Test
    fun testChildType(){
        val update = getStructuralElement()
        assertFalse(update.getViewOfChildren().first() is BodyParam)
        assertTrue(update.getViewOfChildren().first() is ObjectGene)
    }
}
