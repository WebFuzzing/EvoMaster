package org.evomaster.core.problem.rest.builder

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchGeneBuilderTest {

    // -------------------------------------------------------------------------
    // extractAllPaths
    // -------------------------------------------------------------------------

    @Test
    fun `extractAllPaths on null returns empty list`() {
        assertTrue(JsonPatchGeneBuilder.extractAllPaths(null).isEmpty())
    }

    @Test
    fun `extractAllPaths on non-object gene returns empty list`() {
        assertTrue(JsonPatchGeneBuilder.extractAllPaths(StringGene("x")).isEmpty())
        assertTrue(JsonPatchGeneBuilder.extractAllPaths(IntegerGene("n")).isEmpty())
        assertTrue(JsonPatchGeneBuilder.extractAllPaths(BooleanGene("b")).isEmpty())
    }

    @Test
    fun `extractAllPaths on empty ObjectGene returns empty list`() {
        assertTrue(JsonPatchGeneBuilder.extractAllPaths(ObjectGene("r", emptyList())).isEmpty())
    }

    @Test
    fun `extractAllPaths on flat ObjectGene returns one PathInfo per field`() {
        val schema = ObjectGene("resource", listOf(
            StringGene("name"),
            StringGene("email"),
            IntegerGene("age")
        ))
        assertEquals(3, JsonPatchGeneBuilder.extractAllPaths(schema).size)
    }

    @Test
    fun `extractAllPaths produces correct JSON Pointer paths`() {
        val schema = ObjectGene("resource", listOf(StringGene("name"), IntegerGene("age")))
        val pathStrings = JsonPatchGeneBuilder.extractAllPaths(schema).map { it.path }
        assertTrue("/name" in pathStrings)
        assertTrue("/age" in pathStrings)
    }

    @Test
    fun `extractAllPaths produces correctly typed value genes for each field`() {
        val schema = ObjectGene("r", listOf(
            StringGene("name"),
            IntegerGene("count"),
            BooleanGene("flag"),
            LongGene("bigNum"),
            DoubleGene("score"),
            FloatGene("ratio")
        ))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema).associateBy { it.path }

        assertInstanceOf(StringGene::class.java,  paths["/name"]!!.gene)
        assertInstanceOf(IntegerGene::class.java, paths["/count"]!!.gene)
        assertInstanceOf(BooleanGene::class.java, paths["/flag"]!!.gene)
        assertInstanceOf(LongGene::class.java,    paths["/bigNum"]!!.gene)
        assertInstanceOf(DoubleGene::class.java,  paths["/score"]!!.gene)
        assertInstanceOf(FloatGene::class.java,   paths["/ratio"]!!.gene)
    }

    @Test
    fun `extractAllPaths value genes are named value`() {
        val schema = ObjectGene("r", listOf(StringGene("title"), IntegerGene("count")))
        JsonPatchGeneBuilder.extractAllPaths(schema).forEach { assertEquals("value", it.gene.name) }
    }

    @Test
    fun `extractAllPaths value genes are copies not the original field genes`() {
        val nameField = StringGene("name", "original")
        val schema = ObjectGene("r", listOf(nameField))
        val valueGene = JsonPatchGeneBuilder.extractAllPaths(schema).first().gene as StringGene
        valueGene.value = "mutated"
        assertEquals("original", nameField.value)
    }

    @Test
    fun `extractAllPaths on nested ObjectGene produces sub-paths`() {
        val schema = ObjectGene("r", listOf(
            StringGene("title"),
            ObjectGene("address", listOf(StringGene("street"), StringGene("city")))
        ))
        val pathStrings = JsonPatchGeneBuilder.extractAllPaths(schema).map { it.path }
        assertTrue("/title" in pathStrings)
        assertTrue("/address/street" in pathStrings)
        assertTrue("/address/city" in pathStrings)
        assertFalse("/address" in pathStrings)
    }

    @Test
    fun `extractAllPaths on three-level nested ObjectGene produces deep paths`() {
        val schema = ObjectGene("r", listOf(
            ObjectGene("a", listOf(ObjectGene("b", listOf(IntegerGene("c")))))
        ))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema)
        assertEquals(1, paths.size)
        assertEquals("/a/b/c", paths.first().path)
    }

    @Test
    fun `extractAllPaths unwraps OptionalGene transparently`() {
        val schema = ObjectGene("r", listOf(OptionalGene("name", StringGene("name"))))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema)
        assertTrue("/name" in paths.map { it.path })
        assertInstanceOf(StringGene::class.java, paths.first().gene)
    }

    @Test
    fun `extractAllPaths unwraps NullableGene transparently`() {
        val schema = ObjectGene("r", listOf(NullableGene("age", IntegerGene("age"))))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema)
        assertTrue("/age" in paths.map { it.path })
        assertInstanceOf(IntegerGene::class.java, paths.first().gene)
    }

    @Test
    fun `extractAllPaths uses provided prefix`() {
        val schema = ObjectGene("address", listOf(StringGene("city")))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema, prefix = "/base")
        assertEquals("/base/city", paths.first().path)
    }

    // -------------------------------------------------------------------------
    // buildPathValueEntries — returns List<PairGene<EnumGene<String>, Gene>>
    // -------------------------------------------------------------------------

    @Test
    fun `buildPathValueEntries on empty list returns empty`() {
        assertTrue(JsonPatchGeneBuilder.buildPathValueEntries(emptyList()).isEmpty())
    }

    @Test
    fun `buildPathValueEntries on single path returns one PairGene`() {
        val paths = listOf(JsonPatchGeneBuilder.PathInfo("/name", StringGene("value")))
        assertEquals(1, JsonPatchGeneBuilder.buildPathValueEntries(paths).size)
    }

    @Test
    fun `buildPathValueEntries groups same-type paths into one PairGene`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/name", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/email", StringGene("value"))
        )
        assertEquals(1, JsonPatchGeneBuilder.buildPathValueEntries(paths).size)
    }

    @Test
    fun `buildPathValueEntries creates separate PairGenes for different types`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/name", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/age", IntegerGene("value"))
        )
        assertEquals(2, JsonPatchGeneBuilder.buildPathValueEntries(paths).size)
    }

    @Test
    fun `buildPathValueEntries groups three types into three PairGenes`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/name", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/email", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/age", IntegerGene("value")),
            JsonPatchGeneBuilder.PathInfo("/score", DoubleGene("value"))
        )
        assertEquals(3, JsonPatchGeneBuilder.buildPathValueEntries(paths).size)
    }

    @Test
    fun `buildPathValueEntries first of PairGene is EnumGene containing all paths of that type`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/name", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/email", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/bio", StringGene("value"))
        )
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(paths)
        assertEquals(1, entries.size)

        val pathValues = (entries[0].first as EnumGene<*>).values
        assertEquals(3, pathValues.size)
        assertTrue("/name" in pathValues)
        assertTrue("/email" in pathValues)
        assertTrue("/bio" in pathValues)
    }

    @Test
    fun `buildPathValueEntries second of PairGene type matches field type`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/name", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/age", IntegerGene("value"))
        )
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(paths)
        assertTrue(entries.any { it.second is StringGene }, "Expected a StringGene second")
        assertTrue(entries.any { it.second is IntegerGene }, "Expected an IntegerGene second")
    }

    @Test
    fun `buildPathValueEntries PairGenes are named entry_N`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/a", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/b", IntegerGene("value"))
        )
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(paths)
        assertTrue(entries.all { it.name.startsWith("entry_") })
    }

    @Test
    fun `buildPathValueEntries second genes are named value`() {
        val paths = listOf(
            JsonPatchGeneBuilder.PathInfo("/x", StringGene("value")),
            JsonPatchGeneBuilder.PathInfo("/y", IntegerGene("value"))
        )
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(paths)
        entries.forEach { assertEquals("value", it.second.name) }
    }

    @Test
    fun `buildPathValueEntries first genes are named path`() {
        val paths = listOf(JsonPatchGeneBuilder.PathInfo("/x", StringGene("value")))
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(paths)
        entries.forEach { assertEquals("path", it.first.name) }
    }

    // -------------------------------------------------------------------------
    // createValueGene
    // -------------------------------------------------------------------------

    @Test
    fun `createValueGene maps StringGene to a fresh StringGene`() {
        assertInstanceOf(StringGene::class.java, JsonPatchGeneBuilder.createValueGene(StringGene("title")))
    }

    @Test
    fun `createValueGene maps IntegerGene to a fresh IntegerGene`() {
        assertInstanceOf(IntegerGene::class.java, JsonPatchGeneBuilder.createValueGene(IntegerGene("count")))
    }

    @Test
    fun `createValueGene maps BooleanGene to a fresh BooleanGene`() {
        assertInstanceOf(BooleanGene::class.java, JsonPatchGeneBuilder.createValueGene(BooleanGene("active")))
    }

    @Test
    fun `createValueGene maps LongGene to a fresh LongGene`() {
        assertInstanceOf(LongGene::class.java, JsonPatchGeneBuilder.createValueGene(LongGene("bigId")))
    }

    @Test
    fun `createValueGene maps DoubleGene to a fresh DoubleGene`() {
        assertInstanceOf(DoubleGene::class.java, JsonPatchGeneBuilder.createValueGene(DoubleGene("score")))
    }

    @Test
    fun `createValueGene maps FloatGene to a fresh FloatGene`() {
        assertInstanceOf(FloatGene::class.java, JsonPatchGeneBuilder.createValueGene(FloatGene("ratio")))
    }

    @Test
    fun `createValueGene maps BigDecimalGene to a fresh BigDecimalGene`() {
        assertInstanceOf(BigDecimalGene::class.java, JsonPatchGeneBuilder.createValueGene(BigDecimalGene("amount")))
    }

    @Test
    fun `createValueGene maps BigIntegerGene to a fresh BigIntegerGene`() {
        assertInstanceOf(BigIntegerGene::class.java, JsonPatchGeneBuilder.createValueGene(BigIntegerGene("bigNum")))
    }

    @Test
    fun `createValueGene returns null for ObjectGene`() {
        assertNull(JsonPatchGeneBuilder.createValueGene(ObjectGene("nested", emptyList())))
    }

    @Test
    fun `createValueGene unwraps OptionalGene`() {
        assertInstanceOf(StringGene::class.java, JsonPatchGeneBuilder.createValueGene(OptionalGene("opt", StringGene("x"))))
    }

    @Test
    fun `createValueGene unwraps NullableGene`() {
        assertInstanceOf(IntegerGene::class.java, JsonPatchGeneBuilder.createValueGene(NullableGene("nullable", IntegerGene("x"))))
    }

    // -------------------------------------------------------------------------
    // Round-trip: extractAllPaths -> buildPathValueEntries
    // -------------------------------------------------------------------------

    @Test
    fun `round-trip produces one PairGene per distinct field type`() {
        val schema = ObjectGene("resource", listOf(
            StringGene("name"),
            StringGene("email"),
            IntegerGene("age"),
            BooleanGene("active")
        ))
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(
            JsonPatchGeneBuilder.extractAllPaths(schema)
        )
        // String group + Integer group + Boolean group = 3 entries
        assertEquals(3, entries.size)
    }

    @Test
    fun `round-trip groups all string paths into single PairGene`() {
        val schema = ObjectGene("r", listOf(
            StringGene("first"), StringGene("last"), StringGene("email")
        ))
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(
            JsonPatchGeneBuilder.extractAllPaths(schema)
        )
        assertEquals(1, entries.size)
        assertEquals(3, (entries[0].first as EnumGene<*>).values.size)
    }

    @Test
    fun `round-trip with nested schema produces correct sub-paths in pairs`() {
        val schema = ObjectGene("r", listOf(
            StringGene("title"),
            ObjectGene("address", listOf(StringGene("street"), IntegerGene("zip")))
        ))
        val entries = JsonPatchGeneBuilder.buildPathValueEntries(
            JsonPatchGeneBuilder.extractAllPaths(schema)
        )
        // /title + /address/street → StringGene group; /address/zip → IntegerGene group
        assertEquals(2, entries.size)

        val stringEntry = entries.first { it.second is StringGene }
        val intEntry = entries.first { it.second is IntegerGene }

        val stringPaths = (stringEntry.first as EnumGene<*>).values.map { it.toString() }
        assertTrue("/title" in stringPaths)
        assertTrue("/address/street" in stringPaths)

        val intPaths = (intEntry.first as EnumGene<*>).values.map { it.toString() }
        assertTrue("/address/zip" in intPaths)
    }

    // -------------------------------------------------------------------------
    // isSchemaFlat
    // -------------------------------------------------------------------------

    @Test
    fun `isSchemaFlat returns false for null`() {
        assertFalse(JsonPatchGeneBuilder.isSchemaFlat(null))
    }

    @Test
    fun `isSchemaFlat returns false for non-ObjectGene`() {
        assertFalse(JsonPatchGeneBuilder.isSchemaFlat(StringGene("x")))
        assertFalse(JsonPatchGeneBuilder.isSchemaFlat(IntegerGene("n")))
    }

    @Test
    fun `isSchemaFlat returns true for ObjectGene with only primitive fields`() {
        val schema = ObjectGene("r", listOf(StringGene("name"), IntegerGene("age"), BooleanGene("active")))
        assertTrue(JsonPatchGeneBuilder.isSchemaFlat(schema))
    }

    @Test
    fun `isSchemaFlat returns false when any field is nested ObjectGene`() {
        val schema = ObjectGene("r", listOf(
            StringGene("name"),
            ObjectGene("address", listOf(StringGene("street")))
        ))
        assertFalse(JsonPatchGeneBuilder.isSchemaFlat(schema))
    }

    @Test
    fun `isSchemaFlat returns true for empty ObjectGene`() {
        assertTrue(JsonPatchGeneBuilder.isSchemaFlat(ObjectGene("r", emptyList())))
    }

    @Test
    fun `isSchemaFlat unwraps OptionalGene wrapper`() {
        val schema = OptionalGene("r", ObjectGene("r", listOf(StringGene("name"))))
        assertTrue(JsonPatchGeneBuilder.isSchemaFlat(schema))
    }

    @Test
    fun `isSchemaFlat unwraps NullableGene wrapper`() {
        val schema = NullableGene("r", ObjectGene("r", listOf(IntegerGene("count"))))
        assertTrue(JsonPatchGeneBuilder.isSchemaFlat(schema))
    }

    // -------------------------------------------------------------------------
    // depth field in PathInfo
    // -------------------------------------------------------------------------

    @Test
    fun `extractAllPaths depth is 1 for top-level fields`() {
        val schema = ObjectGene("r", listOf(StringGene("name"), IntegerGene("age")))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema)
        paths.forEach { assertEquals(1, it.depth, "Expected depth 1 for path ${it.path}") }
    }

    @Test
    fun `extractAllPaths depth is 2 for one-level nested fields`() {
        val schema = ObjectGene("r", listOf(
            ObjectGene("address", listOf(StringGene("city")))
        ))
        val path = JsonPatchGeneBuilder.extractAllPaths(schema).first()
        assertEquals("/address/city", path.path)
        assertEquals(2, path.depth)
    }

    @Test
    fun `extractAllPaths depth is 3 for two-level nested fields`() {
        val schema = ObjectGene("r", listOf(
            ObjectGene("a", listOf(ObjectGene("b", listOf(IntegerGene("c")))))
        ))
        val path = JsonPatchGeneBuilder.extractAllPaths(schema).first()
        assertEquals(3, path.depth)
    }

    @Test
    fun `extractAllPaths mixed depths are reported correctly`() {
        val schema = ObjectGene("r", listOf(
            StringGene("title"),
            ObjectGene("address", listOf(StringGene("city")))
        ))
        val byPath = JsonPatchGeneBuilder.extractAllPaths(schema).associateBy { it.path }
        assertEquals(1, byPath["/title"]!!.depth)
        assertEquals(2, byPath["/address/city"]!!.depth)
    }

    // -------------------------------------------------------------------------
    // maxDepth parameter
    // -------------------------------------------------------------------------

    @Test
    fun `extractAllPaths with maxDepth 1 returns only top-level fields`() {
        val schema = ObjectGene("r", listOf(
            StringGene("name"),
            ObjectGene("address", listOf(StringGene("city"), IntegerGene("zip")))
        ))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema, maxDepth = 1)
        val pathStrings = paths.map { it.path }
        assertTrue("/name" in pathStrings)
        assertFalse("/address/city" in pathStrings)
        assertFalse("/address/zip" in pathStrings)
    }

    @Test
    fun `extractAllPaths with maxDepth 2 returns up to depth-2 paths`() {
        val schema = ObjectGene("r", listOf(
            ObjectGene("a", listOf(ObjectGene("b", listOf(IntegerGene("c")))))
        ))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema, maxDepth = 2)
        val pathStrings = paths.map { it.path }
        assertFalse("/a/b/c" in pathStrings, "depth-3 path should be excluded")
    }

    @Test
    fun `extractAllPaths flat schema ignores maxDepth and returns only depth-1 paths`() {
        val schema = ObjectGene("r", listOf(StringGene("name"), IntegerGene("age")))
        val paths = JsonPatchGeneBuilder.extractAllPaths(schema, maxDepth = 5)
        paths.forEach { assertEquals(1, it.depth) }
    }
}