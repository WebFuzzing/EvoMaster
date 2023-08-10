package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.regex.RegexGene.Companion.DATABASE_REGEX_PREFIX
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class DbActionGeneBuilderTest {

    @Test
    fun testPostgresSimilarToBuilder() {

        val randomness = Randomness()

        val javaRegex = "/foo/../bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"
        val gene = SqlActionGeneBuilder().buildSimilarToRegexGene("w_id", javaRegex, databaseType = DatabaseType.POSTGRES)

        for (seed in 1..100L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(javaRegex)
            val matcher = pattern.matcher(instance)
            assertTrue(matcher.find())
        }
    }

    @Test

    fun testPostgresLikeBuilder() {

        val randomness = Randomness()

        for (seed in 1..10000L) {
            randomness.updateSeed(seed)

            val likePatterns = listOf("hi", "%foo%", "%foo%x%", "%bar%", "%bar%y%", "%hello%")
            val javaRegexPatterns = listOf("hi", ".*foo.*", ".*foo.*x.*", ".*bar.*", ".*bar.*y.*", ".*hello.*")

            val chosenRegex = randomness.nextInt(0,likePatterns.size-1)

            val gene = SqlActionGeneBuilder().buildLikeRegexGene("f_id", likePatterns[chosenRegex], databaseType = DatabaseType.POSTGRES)

            assertEquals("${DATABASE_REGEX_PREFIX}${likePatterns[chosenRegex]}", gene.sourceRegex)

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            assertTrue(
                        Pattern.compile(javaRegexPatterns[chosenRegex]).matcher(instance).find(), "invalid instance: " + instance
            )
        }
    }


}