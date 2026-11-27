package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.collection.HeterogeneousListGene
import org.evomaster.core.search.gene.collection.ProtectedUniqueArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.impact.impactinfocollection.MutatedGeneWithContext
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.StringGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.collection.ArrayGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.collection.HeterogeneousListGeneImpact
import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

/** Tests inference of DynamoDB initialization actions from failed reads. */
class DynamoDbInsertBuilderTest {

    @Test
    fun buildsTypedActionFromSupportedFailedQuery() {
        val actions = DynamoDbInsertBuilder.buildInsertActions(listOf(validQuery()), Randomness())

        assertEquals(1, actions.size)
        val attributes = actions.single().attributes
        assertEquals("WorldCupPlayers", actions.single().tableName)
        val countryGene = attributes[0].gene as StringGene
        assertEquals("Argentina", countryGene.value)
        assertTrue(countryGene.isMutable())
        assertEquals("10.50", (attributes[1].gene as BigDecimalGene).value.toPlainString())
        assertTrue((attributes[2].gene as BooleanGene).value)
    }

    @Test
    fun buildsMutableSeededStringGeneForTopLevelStringAttributes() {
        val query = DynamoDbFailedQuery(
            "records",
            listOf(
                DynamoDbAttributeValueDto("subject_id", DynamoDbKeyScalarTypeDto.STRING, "Brazil"),
                DynamoDbDtoUtils.fromPlainValue("purpose", "World Cup tickets")
            )
        )

        val action = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()
        val purposeGene = action.attributes.first { it.attributeName == "purpose" }.gene

        assertTrue(purposeGene is StringGene)
        val stringGene = purposeGene as StringGene
        assertEquals("World Cup tickets", stringGene.value)
        assertTrue(stringGene.isMutable())
        stringGene.markAllAsInitialized()
        assertEquals("World Cup tickets", stringGene.value)
    }

    @Test
    fun skipsInvalidQueriesAndRemovesDuplicateActions() {
        val valid = validQuery()
        val invalidNumber = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "ten"))
        )
        val blankTable = DynamoDbFailedQuery(
            "",
            listOf(DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"))
        )
        val missingType = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto().apply {
                attributeName = "country"
                value = "Argentina"
            })
        )
        val emptySet = DynamoDbFailedQuery(
            "WorldCupPlayers",
            listOf(DynamoDbAttributeValueDto().apply {
                attributeName = "tags"
                type = DynamoDbValueTypeDto.STRING_SET
                setValue = emptyList()
            })
        )

        val actions = DynamoDbInsertBuilder.buildInsertActions(
            listOf(valid, valid, invalidNumber, blankTable, missingType, emptySet),
            Randomness()
        )

        assertEquals(1, actions.size)
        assertTrue(
            DynamoDbInsertBuilder.buildInsertActions(
                listOf(valid),
                setOf(actions.single().insertionKey()),
                Randomness()
            ).isEmpty()
        )
    }

    @Test
    fun generatesReproducibleWorldCupKeysWithoutRandomizingInferredValues() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf(
                "recordKey" to DynamoDbKeyScalarTypeDto.STRING,
                "matchNumber" to DynamoDbKeyScalarTypeDto.NUMBER
            )
        }
        val first = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()
        val replay = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()

        assertEquals(
            first.attributes.map { it.gene.getValueAsRawString() },
            replay.attributes.map { it.gene.getValueAsRawString() }
        )
        assertEquals(
            listOf("Argentina", "10.50", "true"),
            first.attributes.take(3).map { it.gene.getValueAsRawString() }
        )
        val recordKey = first.attributes[3].gene as StringGene
        assertFalse(recordKey.value.isEmpty())
        assertEquals(1, recordKey.minLength)
        val matchNumber = first.attributes[4].gene as BigDecimalGene
        assertTrue(matchNumber.isLocallyValid())
        assertTrue(matchNumber.value.precision() <= 38)
    }

    @Test
    fun repeatedFailedReadsCanGenerateFreshKeys() {
        val query = validQuery().apply {
            missingPrimaryKeyAttributes = linkedMapOf("recordKey" to DynamoDbKeyScalarTypeDto.STRING)
        }
        val randomness = Randomness()
        val first = DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).single()
        val second = DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).single()

        assertNotEquals(
            first.attributes.last().gene.getValueAsRawString(),
            second.attributes.last().gene.getValueAsRawString()
        )
    }

    @Test
    fun suppliedKeysDoNotConsumeRandomness() {
        val randomness = Randomness()
        DynamoDbInsertBuilder.buildInsertActions(listOf(validQuery()), randomness)
        assertEquals(Randomness().nextInt(), randomness.nextInt())
    }

    @Test
    fun rejectsUnsupportedMissingKeysAndOverlappingAttributes() {
        for ((name, type) in listOf(
            "captainKey" to DynamoDbKeyScalarTypeDto.BOOLEAN,
            "" to DynamoDbKeyScalarTypeDto.STRING,
            "country" to DynamoDbKeyScalarTypeDto.STRING
        )) {
            val query = validQuery().apply { missingPrimaryKeyAttributes = linkedMapOf(name to type) }
            val randomness = Randomness()
            assertTrue(DynamoDbInsertBuilder.buildInsertActions(listOf(query), randomness).isEmpty())
            assertEquals(Randomness().nextInt(), randomness.nextInt())
        }
    }

    @Test
    fun testNestedLeavesExtractedIntoImmutableGeneTree() {
        val payload = mapOf(
            "profile" to mapOf(
                "role" to "admin",
                "score" to 99.5,
                "active" to true
            ),
            "tags" to listOf("vip", "moderator"),
            "roles" to setOf("forward", "captain"),
            "jerseyNumbers" to setOf(10, 30.5),
            "emptyList" to emptyList<Any?>(),
            "nothing" to null
        )
        val dto = DynamoDbDtoUtils.fromPlainValue("userData", payload)
        val query = DynamoDbFailedQuery("Users", listOf(dto))
        val action = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()
        val rootGene = action.attributes.single().gene as ObjectGene

        assertTrue(rootGene.isFixed)
        assertTrue(rootGene.isMutable())

        val leaves = rootGene.flatView()
        val stringMap = leaves.filterIsInstance<StringGene>().associateBy { it.name }
        assertTrue(stringMap.containsKey("role"))
        assertEquals("admin", stringMap["role"]!!.value)

        val numberGenes = leaves.filterIsInstance<BigDecimalGene>()
        assertEquals(3, numberGenes.size)
        assertTrue(numberGenes.any { it.value == BigDecimal("99.5") })

        val boolGenes = leaves.filterIsInstance<BooleanGene>()
        assertEquals(1, boolGenes.size)
        assertTrue(boolGenes.first().value)

        val tagsGene = rootGene.fixedFields.first { it.name == "tags" } as HeterogeneousListGene
        assertFalse(tagsGene.isFixed)
        assertEquals(2, tagsGene.elements.size)
        assertEquals(2, tagsGene.minSize)

        val rolesGene = rootGene.fixedFields.first { it.name == "roles" } as ProtectedUniqueArrayGene<*>
        assertTrue(rolesGene.template is StringGene)
        assertEquals(2, rolesGene.getViewOfElements().size)
        assertEquals(2, rolesGene.protectedCount)

        val numbersGene = rootGene.fixedFields.first { it.name == "jerseyNumbers" } as ProtectedUniqueArrayGene<*>
        assertTrue(numbersGene.template is BigDecimalGene)
        assertEquals(2, numbersGene.getViewOfElements().size)
        assertEquals(2, numbersGene.protectedCount)

        val emptyListGene = rootGene.fixedFields.first { it.name == "emptyList" } as HeterogeneousListGene
        assertFalse(emptyListGene.isFixed)
        assertTrue(emptyListGene.elements.isEmpty())
        assertTrue(emptyListGene.isMutable())

        val nullGene = rootGene.fixedFields.first { it.name == "nothing" } as ImmutableDataHolderGene
        assertEquals("null", nullGene.value)
        assertFalse(nullGene.inQuotes)

        assertEquals(dto, action.attributes.single().toDto())
    }

    @Test
    fun testHeterogeneousListElementsHaveUniqueNamesAndCreateCollectionImpacts() {
        val tagsDto = DynamoDbDtoUtils.fromPlainValue(
            "tags",
            listOf(
                "world-champion",
                mapOf("tier" to "gold"),
                listOf("nested-list"),
                setOf("left", "right"),
                setOf(1, 2),
                null
            )
        )
        val query = DynamoDbFailedQuery("PlayerCollections", listOf(tagsDto))
        val action = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()
        val tagsGene = action.attributes.single().gene as HeterogeneousListGene

        assertEquals(
            listOf("tags_0", "tags_1", "tags_2", "tags_3", "tags_4", "tags_5"),
            tagsGene.elements.map { it.name }
        )

        val listImpact = ImpactUtils.createGeneImpact(tagsGene, tagsGene.name)
        assertTrue(listImpact is HeterogeneousListGeneImpact)
        assertTrue(ImpactUtils.createGeneImpact(tagsGene.elements[0], "tags_0") is StringGeneImpact)
        assertTrue(ImpactUtils.createGeneImpact(tagsGene.elements[1], "tags_1") is ObjectGeneImpact)
        assertTrue(ImpactUtils.createGeneImpact(tagsGene.elements[2], "tags_2") is HeterogeneousListGeneImpact)
        assertTrue(ImpactUtils.createGeneImpact(tagsGene.elements[3], "tags_3") is ArrayGeneImpact)
        assertTrue(ImpactUtils.createGeneImpact(tagsGene.elements[4], "tags_4") is ArrayGeneImpact)
        assertTrue(ImpactUtils.createGeneImpact(tagsGene.elements[5], "tags_5") is GeneImpact)
    }

    @Test
    fun testTopLevelAndNestedSetPromotionAndMutation() {
        val ssDto = DynamoDbDtoUtils.fromPlainValue("positions", setOf("forward", "captain"))
        val nsDto = DynamoDbDtoUtils.fromPlainValue("scores", setOf(BigDecimal("10.50"), BigDecimal("2")))
        val query = DynamoDbFailedQuery("WorldCupPlayers", listOf(ssDto, nsDto))

        val action = DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single()
        val ssGene = action.attributes[0].gene as ProtectedUniqueArrayGene<*>
        val nsGene = action.attributes[1].gene as ProtectedUniqueArrayGene<*>

        assertTrue(ssGene.template is StringGene)
        assertTrue(nsGene.template is BigDecimalGene)
        assertTrue(ssGene.isMutable())
        assertTrue(nsGene.isMutable())

        (ssGene.getViewOfElements()[0] as StringGene).value = "striker"
        (nsGene.getViewOfElements()[0] as BigDecimalGene).value = BigDecimal("7")

        val updatedSsDto = action.attributes[0].toDto()
        val updatedNsDto = action.attributes[1].toDto()
        assertEquals(DynamoDbValueTypeDto.STRING_SET, updatedSsDto.type)
        assertEquals(listOf("forward", "striker"), updatedSsDto.setValue)
        assertEquals(DynamoDbValueTypeDto.NUMBER_SET, updatedNsDto.type)
        assertEquals(listOf("7", "10.50"), updatedNsDto.setValue)
    }

    @Test
    fun testMutatingPromotedLeafUpdatesOutputDto() {
        val payload = mapOf(
            "profile" to mapOf(
                "role" to "admin",
                "department" to "engineering"
            )
        )
        val dto = DynamoDbDtoUtils.fromPlainValue("profileData", payload)
        val action = DynamoDbInsertBuilder.buildInsertActions(listOf(DynamoDbFailedQuery("Users", listOf(dto))), Randomness()).single()

        val roleGene = action.attributes.single().gene.flatView().filterIsInstance<StringGene>().first { it.name == "role" }
        roleGene.value = "viewer"

        val updatedDto = action.attributes.single().toDto()
        val profileMap = updatedDto.mapValue["profile"]!!.mapValue
        assertEquals("viewer", profileMap["role"]!!.value)
        assertEquals("engineering", profileMap["department"]!!.value)
    }

    @Test
    fun testCopyPreservesStructureAndIsolatesMutations() {
        val payload = mapOf("status" to "PENDING", "step" to 1)
        val dto = DynamoDbDtoUtils.fromPlainValue("task", payload)
        val original = DynamoDbInsertBuilder.buildInsertActions(listOf(DynamoDbFailedQuery("Tasks", listOf(dto))), Randomness()).single()

        val copy = original.copy() as DynamoDbAction
        val copyStatusGene = copy.attributes.single().gene.flatView().filterIsInstance<StringGene>().first { it.name == "status" }
        copyStatusGene.value = "COMPLETED"

        assertEquals("COMPLETED", copy.attributes.single().toDto().mapValue["status"]!!.value)
        assertEquals("PENDING", original.attributes.single().toDto().mapValue["status"]!!.value)
    }

    @Test
    fun testLargeDocumentPromotesAllScalarAndSetLeaves() {
        val entries = linkedMapOf<String, Any?>()
        for (i in 0 until 50) {
            entries["k%02d".format(i)] = "val_$i"
        }
        entries["z50_str_null"] = "null"
        entries["z51_num"] = 42.5
        entries["z52_bool"] = true
        entries["z53_ss"] = setOf("a", "b")
        entries["z54_ns"] = setOf(1, 2)
        entries["z55_empty_list"] = emptyList<Any?>()
        entries["z56_null"] = null

        val dto = DynamoDbDtoUtils.fromPlainValue("doc", entries)
        val action = DynamoDbInsertBuilder.buildInsertActions(listOf(DynamoDbFailedQuery("Items", listOf(dto))), Randomness()).single()
        val objGene = action.attributes.single().gene as ObjectGene

        assertEquals(53, objGene.flatView().filterIsInstance<StringGene>().size)
        val z50 = objGene.fixedFields.first { it.name == "z50_str_null" } as StringGene
        val z51 = objGene.fixedFields.first { it.name == "z51_num" } as BigDecimalGene
        val z52 = objGene.fixedFields.first { it.name == "z52_bool" } as BooleanGene
        val z53 = objGene.fixedFields.first { it.name == "z53_ss" } as ProtectedUniqueArrayGene<*>
        val z54 = objGene.fixedFields.first { it.name == "z54_ns" } as ProtectedUniqueArrayGene<*>
        val z55 = objGene.fixedFields.first { it.name == "z55_empty_list" } as HeterogeneousListGene
        val z56 = objGene.fixedFields.first { it.name == "z56_null" } as ImmutableDataHolderGene

        assertTrue(z50.isMutable())
        assertEquals("null", z50.value)

        assertTrue(z51.isMutable())
        assertEquals(BigDecimal("42.5"), z51.value)

        assertTrue(z52.isMutable())
        assertTrue(z52.value)

        assertTrue(z53.isMutable())
        assertTrue(z53.getViewOfElements().all { it is StringGene })
        assertTrue(z54.isMutable())
        assertTrue(z54.getViewOfElements().all { it is BigDecimalGene })

        assertTrue(z55.isMutable())
        assertTrue(z55.elements.isEmpty())

        assertFalse(z56.isMutable())
        assertEquals("null", z56.value)
        assertFalse(z56.inQuotes)

        val roundTripped = action.attributes.single().toDto()
        assertEquals(dto, roundTripped)
    }

    /** Creates a baseline valid failed query with string, number, and boolean attributes. */
    private fun validQuery(): DynamoDbFailedQuery = DynamoDbFailedQuery(
        "WorldCupPlayers",
        listOf(
            DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"),
            DynamoDbAttributeValueDto("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "10.50"),
            DynamoDbAttributeValueDto("captain", DynamoDbKeyScalarTypeDto.BOOLEAN, "true")
        )
    )

    @Test
    fun converterAndExampleInstantsConstrainedToValidDateTimeGenes() {
        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, format = "date-time") is DateTimeGene)
        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, example = "2026-06-11T19:00:00Z") is DateTimeGene)
        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, format = "custom") is StringGene)
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = ["null"],
        value = [
            "2026-11-16T19:52:39.906338400Z, null, 2026-11-16T19:52:39.906Z",
            "2026-11-15T19:52:04.370890700Z, date-time, 2026-11-15T19:52:04.370Z",
            "99999-01-01T00:00:00Z, date-time, 2020-01-01T00:00:00Z"
        ]
    )
    fun truncatesNanosecondsAndFallsBackOutOfRangeDateTime(input: String, format: String?, expectedRaw: String) {
        val gene = buildPresentGene(DynamoDbValueTypeDto.STRING, input, format) as DateTimeGene
        assertEquals(expectedRaw, gene.getValueAsRawString())
    }

    @Test
    fun buildsIsoLocalTemporalAndUuidGenesForPresentAndMissingKeys() {
        val localDt = buildPresentGene(DynamoDbValueTypeDto.STRING, "2026-06-11T19:00:00", "local-date-time") as DateTimeGene
        val localDate = buildPresentGene(DynamoDbValueTypeDto.STRING, "2026-06-11", "date") as DateGene
        val localTime = buildPresentGene(DynamoDbValueTypeDto.STRING, "19:00:00", "local-time") as TimeGene
        val uuid = buildPresentGene(DynamoDbValueTypeDto.STRING, "123e4567-e89b-12d3-a456-426614174000", "uuid")

        assertEquals(FormatForDatesAndTimes.ISO_LOCAL, localDt.format)
        assertEquals(FormatForDatesAndTimes.ISO_LOCAL, localDate.format)
        assertEquals(FormatForDatesAndTimes.ISO_LOCAL, localTime.format)
        assertTrue(uuid is UUIDGene)

        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, format = "local-date-time") is DateTimeGene)
        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, format = "date") is DateGene)
        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, format = "local-time") is TimeGene)
        assertTrue(buildMissingKeyGene(DynamoDbKeyScalarTypeDto.STRING, format = "uuid") is UUIDGene)
    }

    @Test
    fun respectsIntegralBoundsForFormattedNumbersAndKeepsFloatMutability() {
        val minByte = BigDecimal.valueOf(Byte.MIN_VALUE.toLong())
        val maxByte = BigDecimal.valueOf(Byte.MAX_VALUE.toLong())
        val minShort = BigDecimal.valueOf(Short.MIN_VALUE.toLong())
        val maxShort = BigDecimal.valueOf(Short.MAX_VALUE.toLong())

        val tiny = buildPresentGene(DynamoDbValueTypeDto.NUMBER, "5", "int8") as BigDecimalGene
        assertEquals(minByte, tiny.min)
        assertEquals(maxByte, tiny.max)
        assertEquals(0, tiny.scale)

        val missingShort = buildMissingKeyGene(DynamoDbKeyScalarTypeDto.NUMBER, format = "int16") as BigDecimalGene
        assertEquals(minShort, missingShort.min)
        assertEquals(maxShort, missingShort.max)
        assertEquals(0, missingShort.scale)

        val tinyOutOfRange = buildPresentGene(DynamoDbValueTypeDto.NUMBER, "500", "int8") as BigDecimalGene
        assertTrue(tinyOutOfRange.min!! < minByte)
        assertTrue(tinyOutOfRange.max!! > maxByte)
        assertTrue(tinyOutOfRange.isLocallyValid())

        val big = buildPresentGene(DynamoDbValueTypeDto.NUMBER, "9999999999999999999999999", "integer") as BigDecimalGene
        assertTrue(big.min!! < BigDecimal.valueOf(Long.MIN_VALUE))
        assertTrue(big.max!! > BigDecimal.valueOf(Long.MAX_VALUE))
        assertEquals(0, big.scale)

        val explicitDecimal = buildPresentGene(DynamoDbValueTypeDto.NUMBER, "12.34", "int32") as BigDecimalGene
        val unformattedFloat = buildPresentGene(DynamoDbValueTypeDto.NUMBER, "3.5") as BigDecimalGene
        assertTrue(explicitDecimal.floatingPointMode)
        assertTrue(unformattedFloat.floatingPointMode)
    }

    /** Builds a single-attribute failed query and returns the resulting gene for that attribute. */
    private fun buildPresentGene(
        type: DynamoDbValueTypeDto,
        value: String,
        format: String? = null
    ): Gene {
        val query = DynamoDbFailedQuery(
            "Events",
            listOf(DynamoDbAttributeValueDto("attr", type, value))
        ).apply {
            if (format != null) {
                primaryKeyFormats = linkedMapOf("attr" to format)
            }
        }
        return DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single().attributes.single().gene
    }

    /** Builds a failed query with one missing primary key and returns its generated gene. */
    private fun buildMissingKeyGene(
        type: DynamoDbKeyScalarTypeDto,
        format: String? = null,
        example: String? = null
    ): Gene {
        val query = DynamoDbFailedQuery(
            "Events",
            listOf(DynamoDbAttributeValueDto("seed", DynamoDbValueTypeDto.STRING, "x")),
            linkedMapOf("key" to type)
        ).apply {
            if (format != null) {
                primaryKeyFormats = linkedMapOf("key" to format)
            }
            if (example != null) {
                primaryKeyValueExamples = linkedMapOf("key" to example)
            }
        }
        return DynamoDbInsertBuilder.buildInsertActions(listOf(query), Randomness()).single().attributes.last().gene
    }

    @Test
    fun playerProfilesItemRoundTripsAndSurvives100SeededMutationsAndImpactCollection() {
        val playerProfile = linkedMapOf<String, Any?>(
            "playerName" to "Lionel Messi",
            "country" to "Argentina",
            "jerseyNumber" to BigDecimal("10"),
            "captain" to true,
            "roles" to setOf("captain", "forward", "playmaker"),
            "scoredMinutes" to setOf(BigDecimal("23"), BigDecimal("108")),
            "emptyBadges" to emptyList<Any?>(),
            "emptyExtra" to emptyMap<String, Any?>(),
            "matchTimeline" to listOf(
                "Lusail Final",
                BigDecimal("2022"),
                true,
                null,
                mapOf("opponent" to "France", "penalties" to BigDecimal("4"))
            ),
            "retiredAt" to null
        )
        val dto = DynamoDbDtoUtils.fromPlainValue("profile", playerProfile)
        val action = DynamoDbInsertBuilder.buildInsertActions(
            listOf(DynamoDbFailedQuery("PlayerProfiles", listOf(dto))),
            Randomness()
        ).single()

        // Initial round-trip is lossless
        assertEquals(dto, action.attributes.single().toDto())

        val rootGene = action.attributes.single().gene
        rootGene.markAllAsInitialized()
        val impact = ImpactUtils.createGeneImpact(rootGene, rootGene.name)
        val rand = Randomness().apply { updateSeed(2026L) }
        val injector: Injector = LifecycleInjector.builder()
            .withModules(*arrayOf<Module>(BaseModule(arrayOf("--blackBox", "false"), true)))
            .build().createInjector()
        injector.getInstance(EMConfig::class.java).apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 1000
            seed = 2026
            weightBasedMutationRate = true
        }
        val apc = injector.getInstance(AdaptiveParameterControl::class.java)
        val mwc = injector.getInstance(MutationWeightControl::class.java)

        repeat(100) { step ->
            val previous = rootGene.copy()
            rootGene.standardMutation(rand, apc, mwc)
            assertTrue(rootGene.isLocallyValid(), "Invalid gene at step $step")
            val ctx = MutatedGeneWithContext(
                current = rootGene,
                actionName = action.getName(),
                position = 0,
                previous = previous,
                numOfMutatedGene = 1
            )
            impact.countImpactWithMutatedGeneWithContext(
                ctx,
                noImpactTargets = emptySet(),
                impactTargets = setOf(1),
                improvedTargets = if (step % 5 == 0) setOf(1) else emptySet(),
                onlyManipulation = false
            )
            // Serialization to DTO must always succeed and preserve non-empty sets
            val mutatedDto = action.attributes.single().toDto()
            val mutatedRoles = mutatedDto.mapValue.getValue("roles").setValue
            val mutatedMinutes = mutatedDto.mapValue.getValue("scoredMinutes").setValue
            assertTrue(mutatedRoles.isNotEmpty(), "String set became empty at step $step")
            assertEquals(mutatedRoles.size, mutatedRoles.distinct().size, "Duplicate SS at step $step")
            assertTrue(mutatedMinutes.isNotEmpty(), "Number set became empty at step $step")
            assertEquals(
                mutatedMinutes.size,
                mutatedMinutes.map { it.toBigDecimal().stripTrailingZeros() }.distinct().size,
                "Duplicate NS at step $step"
            )
        }
    }
}
