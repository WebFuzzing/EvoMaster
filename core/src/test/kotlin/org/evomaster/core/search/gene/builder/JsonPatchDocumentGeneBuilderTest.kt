package org.evomaster.core.search.gene.builder

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchFromPathGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchOperationGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathOnlyGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathValueGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchDocumentGeneBuilderTest {

    private val rand = Randomness().apply { updateSeed(42) }

    // -------------------------------------------------------------------------
    // buildOperationsArray — structure
    // -------------------------------------------------------------------------

    @Test
    fun testBuildOperationsArrayDoesNotThrow() {
        assertDoesNotThrow { JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand) }
    }

    @Test
    fun testBuildOperationsArrayProduces6Choices() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        assertEquals(6, array.template.getViewOfChildren().size)
    }

    @Test
    fun testBuildOperationsArrayMinSizeIs1() {
        assertEquals(JsonPatchDocumentGeneBuilder.MIN_SIZE,
            JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand).minSize)
    }

    @Test
    fun testBuildOperationsArrayMaxSizeIs10() {
        assertEquals(JsonPatchDocumentGeneBuilder.DEFAULT_MAX_SIZE,
            JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand).maxSize)
    }

    @Test
    fun testBuildOperationsArrayTemplateIsChoiceGene() {
        assertInstanceOf(ChoiceGene::class.java,
            JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand).template)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — operation order and types
    // -------------------------------------------------------------------------

    private fun children(randomness: Randomness = rand): List<JsonPatchOperationGene> =
        JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = randomness)
            .template.getViewOfChildren().map { it as JsonPatchOperationGene }

    @Test
    fun testFirstChoiceIsRemove() {
        val op = children()[0]
        assertEquals(JsonPatchOperationGene.OP_REMOVE, op.operationName)
        assertInstanceOf(JsonPatchPathOnlyGene::class.java, op)
    }

    @Test
    fun testSecondChoiceIsMove() {
        val op = children()[1]
        assertEquals(JsonPatchOperationGene.OP_MOVE, op.operationName)
        assertInstanceOf(JsonPatchFromPathGene::class.java, op)
    }

    @Test
    fun testThirdChoiceIsCopy() {
        val op = children()[2]
        assertEquals(JsonPatchOperationGene.OP_COPY, op.operationName)
        assertInstanceOf(JsonPatchFromPathGene::class.java, op)
    }

    @Test
    fun testFourthChoiceIsAdd() {
        val op = children()[3]
        assertEquals(JsonPatchOperationGene.OP_ADD, op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    @Test
    fun testFifthChoiceIsReplace() {
        val op = children()[4]
        assertEquals(JsonPatchOperationGene.OP_REPLACE, op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    @Test
    fun testSixthChoiceIsTest() {
        val op = children()[5]
        assertEquals(JsonPatchOperationGene.OP_TEST, op.operationName)
        assertInstanceOf(JsonPatchPathValueGene::class.java, op)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — random paths (no schema)
    // -------------------------------------------------------------------------

    @Test
    fun testRandomPathsAllStartWithSlash() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue(paths.all { it.startsWith("/") }, "All paths must start with '/': $paths")
    }

    @Test
    fun testRandomPathsAreNonEmpty() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        assertTrue((removeOp.pathGene as EnumGene<*>).values.isNotEmpty())
    }

    @Test
    fun testRandomPathsAreDistinct() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertEquals(paths.size, paths.distinct().size, "Paths must be distinct: $paths")
    }

    @Test
    fun testDifferentSeedsProduceDifferentPaths() {
        fun paths(seed: Long): List<String> {
            val r = Randomness().apply { updateSeed(seed) }
            val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = r)
            val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
            return (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        }
        assertNotEquals(paths(1L), paths(999L),
            "Different seeds should (almost certainly) produce different paths")
    }

    @Test
    fun testNoRandomnessArgumentStillProducesValidPaths() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray()
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue(paths.isNotEmpty() && paths.all { it.startsWith("/") },
            "Paths must be valid even without explicit randomness: $paths")
    }

    @Test
    fun testRandomPathsConsistentAcrossAllOps() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        val removePaths = ((array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene)
            .pathGene as EnumGene<*>).values.map { it.toString() }.sorted()
        val addEntry = (array.template.getViewOfChildren()[3] as JsonPatchPathValueGene)
            .pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        val addPaths = (addEntry.first as EnumGene<*>).values.map { it.toString() }.sorted()
        assertEquals(removePaths, addPaths, "Paths must be consistent across all operations")
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray — dual value types (StringGene + IntegerGene)
    // -------------------------------------------------------------------------

    @Test
    fun testPathValueOperationsHoldTwoPairGeneEntries() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        for (idx in 3..5) {
            val op = array.template.getViewOfChildren()[idx] as JsonPatchPathValueGene
            assertEquals(2, op.pathValueChoice.getViewOfChildren().size,
                "Expected StringGene + IntegerGene entries for op at index $idx")
        }
    }

    @Test
    fun testPathValueEntryFirstIsEnumGeneNamedPath() {
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(randomness = rand)
        val addOp = array.template.getViewOfChildren()[3] as JsonPatchPathValueGene
        val entry = addOp.pathValueChoice.getViewOfChildren()[0] as PairGene<*, *>
        assertInstanceOf(EnumGene::class.java, entry.first)
        assertEquals("path", entry.first.name)
    }

    @Test
    fun testDefaultBuildHasStringGeneEntry() {
        val addOp = children()[3] as JsonPatchPathValueGene
        val entries = addOp.pathValueChoice.getViewOfChildren().map { it as PairGene<*, *> }
        assertTrue(entries.any { it.second is StringGene }, "Expected at least one StringGene value entry")
    }

    @Test
    fun testDefaultBuildHasIntegerGeneEntry() {
        val addOp = children()[3] as JsonPatchPathValueGene
        val entries = addOp.pathValueChoice.getViewOfChildren().map { it as PairGene<*, *> }
        assertTrue(entries.any { it.second is IntegerGene }, "Expected at least one IntegerGene value entry")
    }

    @Test
    fun testAllPathValueOpsHaveBothValueTypes() {
        val ops = children()
        for (idx in 3..5) {
            val op = ops[idx] as JsonPatchPathValueGene
            val seconds = op.pathValueChoice.getViewOfChildren().map { (it as PairGene<*, *>).second }
            assertTrue(seconds.any { it is StringGene },  "op[$idx] missing StringGene entry")
            assertTrue(seconds.any { it is IntegerGene }, "op[$idx] missing IntegerGene entry")
        }
    }

    @Test
    fun testBothEntriesHaveTheSamePaths() {
        val addOp = children()[3] as JsonPatchPathValueGene
        val entries = addOp.pathValueChoice.getViewOfChildren().map { it as PairGene<*, *> }
        val pathsFirst  = (entries[0].first as EnumGene<*>).values.map { it.toString() }.sorted()
        val pathsSecond = (entries[1].first as EnumGene<*>).values.map { it.toString() }.sorted()
        assertEquals(pathsFirst, pathsSecond, "Both entries must cover the same set of paths")
    }

    // -------------------------------------------------------------------------
    // extractSchemaFields
    // -------------------------------------------------------------------------

    @Test
    fun testExtractSchemaFieldsFromFlatObjectGene() {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val fields = JsonPatchDocumentGeneBuilder.extractSchemaFields(schema)
        assertEquals(2, fields.size)
        val paths = fields.map { it.path }
        assertTrue("/name" in paths)
        assertTrue("/age" in paths)
    }

    @Test
    fun testExtractSchemaFieldsFromNestedObjectGene() {
        val schema = ObjectGene("body", listOf(
            StringGene("title"),
            ObjectGene("address", listOf(StringGene("street"), StringGene("city")))
        ))
        val fields = JsonPatchDocumentGeneBuilder.extractSchemaFields(schema)
        val paths = fields.map { it.path }
        assertTrue("/title" in paths)
        assertTrue("/address/street" in paths)
        assertTrue("/address/city" in paths)
    }

    @Test
    fun testExtractSchemaFieldsUnwrapsOptionalGene() {
        val schema = ObjectGene("body", listOf(OptionalGene("email", StringGene("email"))))
        val fields = JsonPatchDocumentGeneBuilder.extractSchemaFields(schema)
        assertEquals(1, fields.size)
        assertEquals("/email", fields[0].path)
        assertInstanceOf(StringGene::class.java, fields[0].gene)
    }

    @Test
    fun testExtractSchemaFieldsSkipsCycleObjectGene() {
        val schema = ObjectGene("body", listOf(StringGene("id"), CycleObjectGene("self")))
        val fields = JsonPatchDocumentGeneBuilder.extractSchemaFields(schema)
        assertEquals(1, fields.size)
        assertEquals("/id", fields[0].path)
    }

    @Test
    fun testExtractSchemaFieldsPreservesGeneType() {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("count")))
        val fields = JsonPatchDocumentGeneBuilder.extractSchemaFields(schema)
        val byPath = fields.associateBy { it.path }
        assertInstanceOf(StringGene::class.java, byPath["/name"]!!.gene)
        assertInstanceOf(IntegerGene::class.java, byPath["/count"]!!.gene)
    }

    // -------------------------------------------------------------------------
    // buildOperationsArray with resourceSchema
    // -------------------------------------------------------------------------

    private fun schemaChildren(schema: ObjectGene) =
        JsonPatchDocumentGeneBuilder.buildOperationsArray(resourceSchema = schema)
            .template.getViewOfChildren().map { it as JsonPatchOperationGene }

    @Test
    fun testSchemaPathsAppearInRemoveEnum() {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val removeOp = schemaChildren(schema)[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue("/name" in paths)
        assertTrue("/age" in paths)
    }

    @Test
    fun testSchemaPathsAppearInMoveFromEnum() {
        val schema = ObjectGene("body", listOf(StringGene("a"), StringGene("b")))
        val moveOp = schemaChildren(schema)[1] as JsonPatchFromPathGene
        val paths = (moveOp.fromGene as EnumGene<*>).values.map { it.toString() }
        assertTrue("/a" in paths && "/b" in paths)
    }

    @Test
    fun testSchemaProducesOneEntryPerTypeGroup() {
        val schema = ObjectGene("body", listOf(StringGene("name"), StringGene("desc"), IntegerGene("age")))
        val addOp = schemaChildren(schema)[3] as JsonPatchPathValueGene
        assertEquals(2, addOp.pathValueChoice.getViewOfChildren().size)
    }

    @Test
    fun testSchemaStringGroupHasStringValueGene() {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val addOp = schemaChildren(schema)[3] as JsonPatchPathValueGene
        val entries = addOp.pathValueChoice.getViewOfChildren().map { it as PairGene<*, *> }
        val stringEntry = entries.first { it.second is StringGene }
        val paths = (stringEntry.first as EnumGene<*>).values.map { it.toString() }
        assertTrue("/name" in paths)
        assertFalse("/age" in paths)
    }

    @Test
    fun testSchemaIntegerGroupHasIntegerValueGene() {
        val schema = ObjectGene("body", listOf(StringGene("name"), IntegerGene("age")))
        val addOp = schemaChildren(schema)[3] as JsonPatchPathValueGene
        val entries = addOp.pathValueChoice.getViewOfChildren().map { it as PairGene<*, *> }
        val intEntry = entries.first { it.second is IntegerGene }
        val paths = (intEntry.first as EnumGene<*>).values.map { it.toString() }
        assertTrue("/age" in paths)
        assertFalse("/name" in paths)
    }

    @Test
    fun testEmptySchemaFallsBackToRandomPaths() {
        val schema = ObjectGene("body", emptyList())
        val array = JsonPatchDocumentGeneBuilder.buildOperationsArray(resourceSchema = schema, randomness = rand)
        val removeOp = array.template.getViewOfChildren()[0] as JsonPatchPathOnlyGene
        val paths = (removeOp.pathGene as EnumGene<*>).values.map { it.toString() }
        assertTrue(paths.isNotEmpty() && paths.all { it.startsWith("/") })
    }
}