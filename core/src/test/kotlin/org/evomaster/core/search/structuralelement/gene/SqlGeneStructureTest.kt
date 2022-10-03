package org.evomaster.core.search.structuralelement.gene

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.sql.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class SqlAutoIncrementGeneStructureTest : GeneStructuralElementBaseTest() {

    override fun throwExceptionInRandomnessTest(): Boolean = true

    override fun getCopyFromTemplate(): Gene = SqlAutoIncrementGene("foo")

    override fun assertCopyFrom(base: Gene) {
    }

    override fun getStructuralElement(): SqlAutoIncrementGene = SqlAutoIncrementGene("foo")

    override fun getExpectedChildrenSize(): Int  = 0
}

class SqlForeignKeyGeneStructureTest : GeneStructuralElementBaseTest() {

    override fun throwExceptionInRandomnessTest(): Boolean = false

    override fun getCopyFromTemplate(): Gene = SqlForeignKeyGene("id",1L, "table", false, 1L)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is SqlForeignKeyGene)
        assertEquals(1L, (base as SqlForeignKeyGene).uniqueIdOfPrimaryKey)
    }

    override fun getStructuralElement(): SqlForeignKeyGene = SqlForeignKeyGene("id",1L, "table", false, 0L)

    override fun getExpectedChildrenSize(): Int  = 0
}

class SqlJsonGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene =
        SqlJSONGene("foo", ObjectGene("foo", listOf(IntegerGene("f1", 4), IntegerGene("f2", 5), IntegerGene("f3", 6))))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is SqlJSONGene)
        (base as SqlJSONGene).objectGene.apply {
            assertEquals(4, (fields[0] as IntegerGene).value)
            assertEquals(5, (fields[1] as IntegerGene).value)
            assertEquals(6, (fields[2] as IntegerGene).value)
        }
    }

    override fun getStructuralElement(): SqlJSONGene =
        SqlJSONGene("foo", ObjectGene("foo", listOf(IntegerGene("f1", 1), IntegerGene("f2", 2), IntegerGene("f3", 3))))

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val f2 = root.objectGene.fields[1]
        assertEquals("f2", f2.name)

        val path = mutableListOf<Int>()
        f2.traverseBackIndex(path)
        assertEquals(mutableListOf(0, 1), path)
    }
}

class SqlNullableGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = NullableGene("nullable", IntegerGene("foo", 1))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is NullableGene)
        assertEquals(1, ((base as NullableGene).gene as IntegerGene).value)
    }

    override fun getStructuralElement(): NullableGene = NullableGene("nullable", IntegerGene("foo", 0))

    override fun getExpectedChildrenSize(): Int  = 1
}

class SqlPrimaryKeyGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = SqlPrimaryKeyGene("key","table", IntegerGene("foo", 42), 2L)

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is SqlPrimaryKeyGene)
        assertEquals(42, ((base as SqlPrimaryKeyGene).gene as IntegerGene).value)
    }

    override fun getStructuralElement(): SqlPrimaryKeyGene = SqlPrimaryKeyGene("key","table", IntegerGene("foo", 0), 1L)

    override fun getExpectedChildrenSize(): Int  = 1
}

class SqlUUIDGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene = UUIDGene("uuid", LongGene("m", 2L), LongGene("l", 1L))

    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is UUIDGene)
        (base as UUIDGene).apply {
            assertEquals(2L, mostSigBits.value)
            assertEquals(1L, leastSigBits.value)
        }
    }

    override fun getStructuralElement(): UUIDGene = UUIDGene("uuid", LongGene("m", 0L), LongGene("l", 0L))

    override fun getExpectedChildrenSize(): Int  = 2
}


class SqlXMLGeneStructureTest : GeneStructuralElementBaseTest() {
    override fun getCopyFromTemplate(): Gene =
        SqlXMLGene("foo", ObjectGene("foo", listOf(IntegerGene("f1", 4), IntegerGene("f2", 5), OptionalGene("f3", IntegerGene("f3", 6)))))


    override fun assertCopyFrom(base: Gene) {
        assertTrue(base is SqlXMLGene)
        (base as SqlXMLGene).objectGene.apply {
            assertEquals(4, (fields[0] as IntegerGene).value)
            assertEquals(5, (fields[1] as IntegerGene).value)
            assertEquals(6, ((fields[2] as OptionalGene).gene as IntegerGene).value)
        }
    }

    override fun getStructuralElement(): SqlXMLGene =
        SqlXMLGene("foo", ObjectGene("foo", listOf(IntegerGene("f1", 1), IntegerGene("f2", 2), OptionalGene("f3", IntegerGene("f3", 3)))))

    override fun getExpectedChildrenSize(): Int  = 1

    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val f3 = (root.objectGene.fields[2] as OptionalGene).gene
        assertEquals("f3", f3.name)

        val path = mutableListOf<Int>()
        f3.traverseBackIndex(path)
        assertEquals(mutableListOf(0, 2, 0), path)
    }
}
