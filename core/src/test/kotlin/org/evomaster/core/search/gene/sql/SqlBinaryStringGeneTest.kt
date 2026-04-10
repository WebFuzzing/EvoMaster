package org.evomaster.core.search.gene.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.utils.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SqlBinaryStringGeneTest {

    @Test
    fun testPrintEmptyBinaryString() {
        val gene = SqlBinaryStringGene("bin", databaseType = DatabaseType.POSTGRES)
        assertEquals("\"\\x\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testPrintNonEmptyBinaryStringPostgres() {
        val gene = SqlBinaryStringGene("bin", databaseType = DatabaseType.POSTGRES)
        val arrayGene = gene.getViewOfChildren()[0] as ArrayGene<IntegerGene>
        arrayGene.addElement(IntegerGene("g0", value = 10)) // 0a
        arrayGene.addElement(IntegerGene("g1", value = 255)) // ff
        assertEquals("\"\\x0aff\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testPrintNonEmptyBinaryStringH2() {
        val gene = SqlBinaryStringGene("bin", databaseType = DatabaseType.H2)
        val arrayGene = gene.getViewOfChildren()[0] as ArrayGene<IntegerGene>
        arrayGene.addElement(IntegerGene("g0", value = 10)) // 0a
        arrayGene.addElement(IntegerGene("g1", value = 255)) // ff
        assertEquals("X${SINGLE_APOSTROPHE_PLACEHOLDER}0aff${SINGLE_APOSTROPHE_PLACEHOLDER}", gene.getValueAsPrintableString())
    }

    @Test
    fun testUnsafeCopyValueFromModifiesDatabaseType() {
        val gene1 = SqlBinaryStringGene(
            name = "bin1",
            databaseType = DatabaseType.POSTGRES
        )
        val gene2 = SqlBinaryStringGene(
            name = "bin2",
            databaseType = DatabaseType.MYSQL
        )

        val ok = gene1.unsafeCopyValueFrom(gene2)

        assertTrue(ok)
        assertEquals(DatabaseType.MYSQL, gene1.databaseType)
    }
}
