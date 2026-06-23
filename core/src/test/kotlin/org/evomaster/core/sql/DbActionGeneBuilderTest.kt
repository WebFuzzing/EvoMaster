package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.sql.schema.*
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class DbActionGeneBuilderTest {

    @Test
    fun testMultiColumnForeignKey() {
        val foreignKeyColumn1 = Column("fkCol1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val foreignKeyColumn2 = Column("fkCol2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)

        val targetColumn1 = Column("targetColumn1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val targetColumn2 = Column("targetColumn2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)

        val targetTableId = TableId("target_table")

        val fk = ForeignKey(
            sourceColumns = listOf(foreignKeyColumn1, foreignKeyColumn2),
            targetTableId = targetTableId,
            targetColumns = listOf(targetColumn1, targetColumn2)
        )

        val table = Table(
            id = TableId("source_table"),
            columns = setOf(foreignKeyColumn1, foreignKeyColumn2),
            foreignKeys = setOf(fk)
        )

        val builder = SqlActionGeneBuilder()

        val gene1 = builder.buildGene(1L, table, foreignKeyColumn1) as SqlForeignKeyGene
        val gene2 = builder.buildGene(1L, table, foreignKeyColumn2) as SqlForeignKeyGene

        assertEquals("fkCol1",gene1.name)
        assertEquals(targetTableId, gene1.targetTable)
        assertEquals("targetColumn1", gene1.targetColumn)

        assertEquals("fkCol2",gene2.name)
        assertEquals(targetTableId, gene2.targetTable)
        assertEquals("targetColumn2", gene2.targetColumn)
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseType::class, names = ["H2", "MYSQL", "POSTGRES"])
    fun testSimilarToBuilder(databaseType: DatabaseType ) {

        val randomness = Randomness()

        val javaRegex = "/foo/../bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"
        val gene = SqlActionGeneBuilder().buildSimilarToRegexGene("w_id", javaRegex, databaseType = databaseType)

        for (seed in 1..100L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(javaRegex)
            val matcher = pattern.matcher(instance)
            assertTrue(matcher.find())
        }
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseType::class, names = ["H2", "MYSQL", "POSTGRES"])
    fun testPostgresLikeBuilder(databaseType: DatabaseType) {

        val randomness = Randomness()

        for (seed in 1..10000L) {
            randomness.updateSeed(seed)

            val likePatterns = listOf("hi", "%foo%", "%foo%x%", "%bar%", "%bar%y%", "%hello%")
            val javaRegexPatterns = listOf("hi", ".*foo.*", ".*foo.*x.*", ".*bar.*", ".*bar.*y.*", ".*hello.*")

            val chosenRegex = randomness.nextInt(0,likePatterns.size-1)

            val gene = SqlActionGeneBuilder().buildLikeRegexGene("f_id", likePatterns[chosenRegex], databaseType = databaseType)

            assertEquals(likePatterns[chosenRegex], gene.sourceRegex)

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            assertTrue(Pattern.compile(javaRegexPatterns[chosenRegex])
                            .matcher(instance).find(),
                "invalid instance: $instance"
            )
        }
    }


}
