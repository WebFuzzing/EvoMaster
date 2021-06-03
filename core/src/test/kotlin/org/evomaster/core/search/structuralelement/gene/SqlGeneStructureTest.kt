package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.gene.sql.*
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class SqlAutoIncrementGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlAutoIncrementGene = SqlAutoIncrementGene("foo")

    override fun getExpectedChildrenSize(): Int  = 0
}

class SqlForeignKeyGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlForeignKeyGene = SqlForeignKeyGene("id",1L, "table", false, 0L)

    override fun getExpectedChildrenSize(): Int  = 0
}

class SqlJsonGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlJSONGene = SqlJSONGene("foo", ObjectGene("foo", listOf(IntegerGene("f1"), IntegerGene("f2"), IntegerGene("f3"))))

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        Assertions.assertEquals(root, root.getRoot())

        val f2 = root.objectGene.fields[1]
        Assertions.assertEquals("f2", f2.name)

        val path = mutableListOf<Int>()
        f2.traverseBackIndex(path)
        Assertions.assertEquals(mutableListOf(0, 1), path)
    }
}

class SqlNullableStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlNullable = SqlNullable("nullable",IntegerGene("foo"))

    override fun getExpectedChildrenSize(): Int  = 1
}

class SqlPrimaryKeyGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlPrimaryKeyGene = SqlPrimaryKeyGene("key","table", IntegerGene("foo"), 1L)

    override fun getExpectedChildrenSize(): Int  = 1
}

class SqlUUIDGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlUUIDGene = SqlUUIDGene("uuid")

    override fun getExpectedChildrenSize(): Int  = 2
}


class SqlXMLGeneStructureTest : StructuralElementBaseTest() {
    override fun getStructuralElement(): SqlXMLGene = SqlXMLGene("foo", ObjectGene("foo", listOf(IntegerGene("f1"), IntegerGene("f2"), OptionalGene("f3", IntegerGene("f3")))))

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        Assertions.assertEquals(root, root.getRoot())

        val f3 = (root.objectGene.fields[2] as OptionalGene).gene
        Assertions.assertEquals("f3", f3.name)

        val path = mutableListOf<Int>()
        f3.traverseBackIndex(path)
        Assertions.assertEquals(mutableListOf(0, 2, 0), path)
    }
}
