package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.regex.RegexGene.Companion.DATABASE_REGEX_PREFIX
import org.evomaster.core.search.gene.regex.RegexGene.Companion.DATABASE_REGEX_SEPARATOR
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class DbActionGeneBuilderTest {

    @Test
    fun testPostgresSimilarToBuilder() {

        val randomness = Randomness()

        val javaRegex = "/foo/../bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?"
        val gene = DbActionGeneBuilder().buildSimilarToRegexGene("w_id", listOf(javaRegex), databaseType = DatabaseType.POSTGRES)

        for (seed in 1..100L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(javaRegex)
            val matcher = pattern.matcher(instance)
            Assertions.assertTrue(matcher.find())
        }
    }

    @Test

    fun testPostgresLikeBuilder() {

        val randomness = Randomness()

        val likePatterns = listOf("hi", "%foo%", "%foo%x%", "%bar%", "%bar%y%", "%hello%")
        val javaRegexPatterns = listOf("hi", ".*foo.*", ".*foo.*x.*", ".*bar.*", ".*bar.*y.*", ".*hello.*")
        val gene = DbActionGeneBuilder().buildLikeRegexGene("f_id", likePatterns, databaseType = DatabaseType.POSTGRES)

        assertEquals("${DATABASE_REGEX_PREFIX}${likePatterns.joinToString(DATABASE_REGEX_SEPARATOR)}", gene.sourceRegex)

        for (seed in 1..10000L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            assertTrue(
                    javaRegexPatterns.stream().anyMatch {
                        Pattern.compile(it).matcher(instance).find()
                    },
                    "invalid instance: " + instance
            )
        }
    }


}