package org.evomaster.core.search.gene

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.BigIntegerGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger

class ArrayGeneTest {

    private data class MutationServices(
        val randomness: Randomness,
        val apc: AdaptiveParameterControl,
        val mwc: MutationWeightControl
    )

    private fun createMutationServices(): MutationServices {
        val injector: Injector = LifecycleInjector.builder()
            .withModules(*arrayOf<Module>(BaseModule(arrayOf("--blackBox", "false"), true)))
            .build().createInjector()
        injector.getInstance(EMConfig::class.java).apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 100
            seed = 666
            weightBasedMutationRate = true
        }
        return MutationServices(
            randomness = injector.getInstance(Randomness::class.java).apply { updateSeed(42) },
            apc = injector.getInstance(AdaptiveParameterControl::class.java),
            mwc = injector.getInstance(MutationWeightControl::class.java)
        )
    }

    @Test
    fun testGene() {
        val gene = ArrayGene("array", template=BooleanGene("boolean"))
        assertEquals(0, gene.getViewOfChildren().size)
    }

    @Test
    fun testGeneMinSize() {
        val gene = ArrayGene("array", minSize = 1, maxSize =10, template=BooleanGene("boolean"))
        assertEquals(0, gene.getViewOfChildren().size)
        val randomness = Randomness()
        gene.randomize(randomness, tryToForceNewValue = true)
        assertTrue(gene.getViewOfChildren().isNotEmpty())
    }

    /**
     * Verifies that an ArrayGene with uniqueElements = true detects and rejects duplicate elements
     * while accepting distinct ones.
     */
    private fun <T : Gene> assertUniqueEnforcement(
        template: T,
        initial: List<T>,
        duplicates: List<T> = initial.map { it.copy() as T },
        distinct: T? = null
    ) {
        val array = ArrayGene(
            "array",
            minSize = 1,
            maxSize = 10,
            template = template,
            uniqueElements = true,
            elements = initial.toMutableList()
        )

        assertTrue(array.isElementApplicableToUniqueCheck(template))
        duplicates.forEach { duplicate ->
            assertTrue(array.doesExist(duplicate))
            assertThrows<IllegalArgumentException> { array.addElement(duplicate) }
        }
        if (distinct != null) {
            assertFalse(array.doesExist(distinct))
            array.addElement(distinct)
            assertEquals(initial.size + 1, array.getViewOfElements().size)
        }
    }

    @Test
    fun testUniqueBoolean() {
        assertUniqueEnforcement(
            template = BooleanGene("qualified"),
            initial = listOf(BooleanGene("argentina", true), BooleanGene("italy", false))
        )
    }

    @Test
    fun testUniqueInteger() {
        assertUniqueEnforcement(
            template = IntegerGene("age"),
            initial = listOf(IntegerGene("messiAge", 35), IntegerGene("mbappeAge", 23)),
            distinct = IntegerGene("modricAge", 37)
        )
    }

    @Test
    fun testUniqueLong() {
        assertUniqueEnforcement(
            template = LongGene("viewers"),
            initial = listOf(LongGene("semiFinal", 500_000_000L), LongGene("final", 1_500_000_000L)),
            distinct = LongGene("openingMatch", 250_000_000L)
        )
    }

    @Test
    fun testUniqueDouble() {
        assertUniqueEnforcement(
            template = DoubleGene("goalsPerMatch"),
            initial = listOf(DoubleGene("argentinaAvg", 2.14), DoubleGene("franceAvg", 2.29)),
            distinct = DoubleGene("brazilAvg", 1.6)
        )
    }

    @Test
    fun testUniqueFloat() {
        assertUniqueEnforcement(
            template = FloatGene("height"),
            initial = listOf(FloatGene("messiHeight", 1.70f), FloatGene("martinezHeight", 1.95f)),
            distinct = FloatGene("mbappeHeight", 1.78f)
        )
    }

    @Test
    fun testUniqueBigInteger() {
        assertUniqueEnforcement(
            template = BigIntegerGene("attendance"),
            initial = listOf(
                BigIntegerGene("lusailFinal", BigInteger("88966")),
                BigIntegerGene("maracanaRecord", BigInteger("173850"))
            ),
            distinct = BigIntegerGene("aztecaFinal", BigInteger("114600"))
        )
    }

    @Test
    fun testUniqueBigDecimal() {
        val highPrecision = BigDecimal("123456789012345678901234567890.12345678")
        assertUniqueEnforcement(
            template = BigDecimalGene("rating"),
            initial = listOf(
                BigDecimalGene("messiRating", BigDecimal("9.5")),
                BigDecimalGene("mbappeRating", highPrecision)
            ),
            duplicates = listOf(
                BigDecimalGene("dupMessi", BigDecimal("9.5")),
                BigDecimalGene("equivalentScaleMessi", BigDecimal("9.50")),
                BigDecimalGene("dupHighPrecision", BigDecimal("123456789012345678901234567890.12345678"))
            ),
            distinct = BigDecimalGene("modricRating", BigDecimal("8.75"))
        )
    }

    @Test
    fun testUniqueString() {
        assertUniqueEnforcement(
            template = StringGene("country"),
            initial = listOf(StringGene("champion", "Argentina"), StringGene("runnerUp", "France")),
            distinct = StringGene("thirdPlace", "Croatia")
        )
    }

    @Test
    fun testUniqueEnum() {
        val template = EnumGene("stage", listOf("GROUP", "SEMIFINAL", "FINAL"))
        assertUniqueEnforcement(
            template = template,
            initial = listOf(
                (template.copy() as EnumGene<String>).apply { index = 0 },
                (template.copy() as EnumGene<String>).apply { index = 1 }
            ),
            distinct = (template.copy() as EnumGene<String>).apply { index = 2 }
        )
    }

    @Test
    fun testUniqueNotApplicableForObjectGene() {
        val playerTemplate = ObjectGene("player", listOf(StringGene("country", "Argentina")))
        val squad = ArrayGene("squad", template = playerTemplate, uniqueElements = true)
        assertFalse(squad.isElementApplicableToUniqueCheck(playerTemplate))
    }

    @Test
    fun testUnsafeCopyValueFromDeduplicatesBigDecimalElements() {
        val sourceRatings = ArrayGene(
            "allRatings",
            template = BigDecimalGene("rating"),
            uniqueElements = false,
            elements = mutableListOf(
                BigDecimalGene("rating1", BigDecimal("9.5")),
                BigDecimalGene("rating2", BigDecimal("9.50")),
                BigDecimalGene("rating3", BigDecimal("8.0"))
            )
        )
        val uniqueRatings = ArrayGene(
            "uniqueRatings",
            template = BigDecimalGene("rating"),
            uniqueElements = true
        )

        assertTrue(uniqueRatings.unsafeCopyValueFrom(sourceRatings))
        assertEquals(2, uniqueRatings.getViewOfElements().size)
        assertTrue(uniqueRatings.doesExist(BigDecimalGene("checkNinePointFive", BigDecimal("9.5"))))
        assertTrue(uniqueRatings.doesExist(BigDecimalGene("checkEight", BigDecimal("8.0"))))
    }

    @Test
    fun testRandomizeUniqueBigDecimalElements() {
        val randomness = Randomness().apply { updateSeed(666) }
        val uniqueAges = ArrayGene(
            "playerAges",
            minSize = 3,
            maxSize = 5,
            template = BigDecimalGene("age", min = BigDecimal("18"), max = BigDecimal("40"), scale = 0),
            uniqueElements = true
        )

        uniqueAges.randomize(randomness, tryToForceNewValue = false)

        val elements = uniqueAges.getViewOfElements()
        assertTrue(elements.size in 3..5)
        for (i in elements.indices) {
            for (j in i + 1 until elements.size) {
                assertFalse(
                    elements[i].containsSameValueAs(elements[j]),
                    "Expected unique elements, but found duplicate values ${elements[i].value} and ${elements[j].value}"
                )
            }
        }
    }

    @Test
    fun testSetValueBasedOn(){
        val gene = ArrayGene(
            "array",
            minSize = 1,
            maxSize = 3,
            template= StringGene("string"),
            uniqueElements = true,
            elements = mutableListOf(StringGene("foo_element", "foo"), StringGene("bar_element", "bar"))
        )

        assertEquals(2, gene.getViewOfChildren().size)

        gene.unsafeSetFromStringValue("baz, qux, quux")

        assertEquals(3, gene.getViewOfChildren().size)
        assertEquals("baz", gene.getViewOfChildren()[0].getValueAsRawString())
    }

    @Test
    fun testJsonSerializationWithBrackets(){
        val intArray = ArrayGene(
            "numbers",
            template = IntegerGene("num"),
            elements = mutableListOf(
                IntegerGene("num1", 10),
                IntegerGene("num2", 20),
                IntegerGene("num3", 30)
            )
        )

        val jsonOutput = intArray.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON)

        assertEquals("[10, 20, 30]", jsonOutput)

        assertTrue(jsonOutput.startsWith("["), "JSON must begin with an opening bracket")
        assertTrue(jsonOutput.endsWith("]"), "JSON must end with a closing bracket")
        assertTrue(jsonOutput.contains(", "), "JSON must contain comma separators")
    }

    @Test
    fun testXmlSerializationEmptyArray(){
        // Test to verify that an empty array in XML does not generate square brackets
        val emptyArray = ArrayGene(
            "items",
            template = StringGene("item"),
            elements = mutableListOf()
        )

        val xmlOutput = emptyArray.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("", xmlOutput)
    }

    @Test
    fun testXmlSerializationWithObjects(){
        val objectArray = ArrayGene(
            "people",
            template = ObjectGene("person", listOf(
                StringGene("name", ""),
                IntegerGene("age", 0)
            )),
            elements = mutableListOf(
                ObjectGene("person", listOf(
                    StringGene("name", "Alice"),
                    IntegerGene("age", 30)
                )),
                ObjectGene("person", listOf(
                    StringGene("name", "Bob"),
                    IntegerGene("age", 25)
                ))
            )
        )

        val xmlOutput = objectArray.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)

        val expected = "<person><name>Alice</name><age>30</age></person><person><name>Bob</name><age>25</age></person>"
        assertEquals(expected, xmlOutput)

        assertTrue(!xmlOutput.contains("["), "XML with objects should not contain square brackets")
        assertTrue(!xmlOutput.contains("]"), "XML with objects should not contain square brackets")
    }

    @Test
    fun testXmlArrayInsideObjectGene(){
        val arrayOfStrings = ArrayGene(
            "items",
            template = StringGene("item"),
            elements = mutableListOf(
                StringGene("item1", "value1"),
                StringGene("item2", "value2")
            )
        )

        val root = ObjectGene("root", listOf(arrayOfStrings))
        val xmlOutput = root.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)

        assertEquals("<root><items><item1>value1</item1><item2>value2</item2></items></root>", xmlOutput)

        assertTrue(!xmlOutput.contains("["), "XML should not contain square brackets")
        assertTrue(!xmlOutput.contains("]"), "XML should not contain square brackets")
        assertTrue(!xmlOutput.contains(", "), "XML must not contain commas with spaces")
    }

    @Test
    fun testDoesExistWithDifferentNumericGeneTypes() {
        val bigIntegerGeneValue = java.math.BigInteger.valueOf(42L)
        val bigIntegerGene = BigIntegerGene("bigInt", bigIntegerGeneValue)

        val integerGeneValue = 42
        val integerGene = IntegerGene("int", integerGeneValue)

        val choiceGene = ChoiceGene("choice", listOf(bigIntegerGene, integerGene))

        val arrayWithChoiceGenes = ArrayGene(
            "arrayOfChoiceGenes",
            template = ChoiceGene("template", listOf(bigIntegerGene, integerGene)),
            elements = mutableListOf(choiceGene)
        )

        val anotherChoiceGene = ChoiceGene("choice", listOf(bigIntegerGene, integerGene))

        choiceGene.selectActiveGene(0)
        anotherChoiceGene.selectActiveGene(1)

        assertFalse(arrayWithChoiceGenes.doesExist(anotherChoiceGene))

    }

    @Test
    fun testLocallyValidRejectsDuplicateElementsWhenUnique() {
        val uniqueWithDuplicates = ArrayGene(
            "roles",
            template = StringGene("role"),
            uniqueElements = true,
            elements = mutableListOf(
                StringGene("role1", "captain"),
                StringGene("role2", "captain")
            )
        )
        assertFalse(uniqueWithDuplicates.isLocallyValid())

        val nonUniqueWithDuplicates = ArrayGene(
            "roles",
            template = StringGene("role"),
            uniqueElements = false,
            elements = mutableListOf(
                StringGene("role1", "captain"),
                StringGene("role2", "captain")
            )
        )
        assertTrue(nonUniqueWithDuplicates.isLocallyValid())

        val uniqueRoles = ArrayGene(
            "roles",
            template = StringGene("role"),
            uniqueElements = true,
            elements = mutableListOf(
                StringGene("role1", "captain"),
                StringGene("role2", "striker")
            )
        )
        assertTrue(uniqueRoles.isLocallyValid())

        uniqueRoles.getViewOfElements()[1].value = "captain"
        assertFalse(uniqueRoles.isLocallyValid())
    }

    @Test
    fun testStandardMutationPreservesUniquenessForStringElements() {
        val services = createMutationServices()
        val template = StringGene("role", minLength = 0, maxLength = 1)
        val roles = ArrayGene(
            "roles",
            template = template,
            uniqueElements = true,
            minSize = 2,
            maxSize = 2,
            elements = mutableListOf(
                StringGene("role1", "", minLength = 0, maxLength = 1),
                StringGene("role2", "a", minLength = 0, maxLength = 1)
            )
        ).apply { doInitialize() }

        repeat(200) {
            roles.standardMutation(services.randomness, services.apc, services.mwc)
            assertTrue(roles.isLocallyValid())
            val values = roles.getViewOfElements().map { it.value }
            assertEquals(2, values.size)
            assertEquals(values.size, values.toSet().size)
        }
    }

    @Test
    fun testStandardMutationRepairsDuplicatesWhenRetriesExhausted() {
        val services = createMutationServices()
        val positions = listOf("GK", "MF", "FW")
        val template = EnumGene("position", positions)

        for (strategy in listOf(
            SubsetGeneMutationSelectionStrategy.DEFAULT,
            SubsetGeneMutationSelectionStrategy.DETERMINISTIC_WEIGHT
        )) {
            repeat(30) {
                val lineup = ArrayGene(
                    "lineup",
                    template = template,
                    uniqueElements = true,
                    maxSize = 3,
                    elements = mutableListOf(
                        EnumGene("pos1", positions, 0),
                        EnumGene("pos2", positions, 1),
                        EnumGene("pos3", positions, 2)
                    )
                ).apply { doInitialize() }

                lineup.standardMutation(
                    services.randomness,
                    services.apc,
                    services.mwc,
                    childrenToMutateSelectionStrategy = strategy
                )
                assertTrue(lineup.isLocallyValid())
                val values = lineup.getViewOfElements().map { it.getValueAsRawString() }
                assertEquals(values.size, values.toSet().size)
            }
        }
    }

    @Test
    fun testRepairRemovesDuplicateElementsWhenUnique() {
        val roles = ArrayGene(
            "roles",
            template = StringGene("role"),
            uniqueElements = true,
            elements = mutableListOf(
                StringGene("role1", "captain"),
                StringGene("role2", "captain"),
                StringGene("role3", "striker")
            )
        )

        assertFalse(roles.isLocallyValid())
        roles.repair()
        assertTrue(roles.isLocallyValid())
        assertEquals(listOf("captain", "striker"), roles.getViewOfElements().map { it.value })
    }

    @Test
    fun testSetFromStringValueRevertsOnDuplicatesWhenUnique() {
        val roles = ArrayGene(
            "roles",
            minSize = 1,
            maxSize = 3,
            template = StringGene("role"),
            uniqueElements = true,
            elements = mutableListOf(
                StringGene("role1", "captain"),
                StringGene("role2", "striker")
            )
        ).apply { doInitialize() }

        assertFalse(roles.setFromStringValue("captain, captain"))
        assertTrue(roles.isLocallyValid())
        assertEquals(listOf("captain", "striker"), roles.getViewOfElements().map { it.value })
    }

}
