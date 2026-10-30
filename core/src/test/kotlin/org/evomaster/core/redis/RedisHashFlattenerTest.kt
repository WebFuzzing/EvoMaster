package org.evomaster.core.redis

import org.evomaster.core.database.redis.RedisHashFlattener
import org.evomaster.core.database.redis.RedisQueryField
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisHashFlattenerTest {

    private fun string(name: String, value: String) = StringGene(name, value)

    private fun address(name: String, city: String, zip: Int) = ObjectGene(
        name, listOf(
            OptionalGene("city", string("city", city)),
            OptionalGene("zip", IntegerGene("zip", zip))
        )
    )

    @Test
    fun testSimpleGeneIsASingleFieldNamedAfterTheProperty() {
        assertEquals(listOf("name" to "bob"), RedisHashFlattener.flatten("name", string("name", "bob")))
    }

    @Test
    fun testInactiveOptionalGeneIsNotStored() {
        val gene = OptionalGene("name", string("name", "bob"), isActive = false)

        assertEquals(emptyList<Pair<String, String>>(), RedisHashFlattener.flatten("name", gene))
    }

    @Test
    fun testInactiveNullableGeneIsNotStored() {
        val gene = NullableGene("name", string("name", "bob"), isActive = false)

        assertEquals(emptyList<Pair<String, String>>(), RedisHashFlattener.flatten("name", gene))
    }

    @Test
    fun testActiveOptionalGeneIsStoredAsItsInnerGene() {
        val gene = OptionalGene("name", string("name", "bob"))

        assertEquals(listOf("name" to "bob"), RedisHashFlattener.flatten("name", gene))
    }

    @Test
    fun testNestedObjectIsStoredOneFieldPerLeafWithDottedPath() {
        assertEquals(
            listOf("address.city" to "Rosario", "address.zip" to "2000"),
            RedisHashFlattener.flatten("address", address("address", "Rosario", 2000))
        )
    }

    @Test
    fun testInactiveLeafOfANestedObjectIsLeftOut() {
        val gene = ObjectGene(
            "address", listOf(
                OptionalGene("city", string("city", "Rosario"), isActive = false),
                OptionalGene("zip", IntegerGene("zip", 2000))
            )
        )

        assertEquals(listOf("address.zip" to "2000"), RedisHashFlattener.flatten("address", gene))
    }

    @Test
    fun testInactiveNestedObjectIsLeftOutAsAWhole() {
        val gene = OptionalGene("address", address("address", "Rosario", 2000), isActive = false)

        assertEquals(emptyList<Pair<String, String>>(), RedisHashFlattener.flatten("address", gene))
    }

    @Test
    fun testObjectNestedInsideAnObjectKeepsTheWholePath() {
        val gene = ObjectGene("person", listOf(OptionalGene("home", address("home", "Rosario", 2000))))

        assertEquals(
            listOf("person.home.city" to "Rosario", "person.home.zip" to "2000"),
            RedisHashFlattener.flatten("person", gene)
        )
    }

    @Test
    fun testListOfSimpleValuesIsStoredWithIndexedPaths() {
        val gene = ArrayGene("tags", string("tags", ""), elements = mutableListOf(string("tags", "a"), string("tags", "b")))

        assertEquals(listOf("tags.[0]" to "a", "tags.[1]" to "b"), RedisHashFlattener.flatten("tags", gene))
    }

    @Test
    fun testListOfObjectsIsStoredWithIndexedAndDottedPaths() {
        val template = address("addrs", "", 0)
        val gene = ArrayGene(
            "addrs", template,
            elements = mutableListOf(address("addrs", "X", 1), address("addrs", "Y", 2))
        )

        assertEquals(
            listOf("addrs.[0].city" to "X", "addrs.[0].zip" to "1", "addrs.[1].city" to "Y", "addrs.[1].zip" to "2"),
            RedisHashFlattener.flatten("addrs", gene)
        )
    }

    @Test
    fun testEmptyListIsNotStored() {
        val gene = ArrayGene("tags", string("tags", ""))

        assertEquals(emptyList<Pair<String, String>>(), RedisHashFlattener.flatten("tags", gene))
    }

    @Test
    fun testMapIsStoredWithItsKeysAsPaths() {
        val template = PairGene("template", string("key", ""), string("value", ""))
        val gene = FixedMapGene(
            "attrs", template,
            elements = mutableListOf(
                PairGene("e1", string("key", "k1"), string("value", "v1")),
                PairGene("e2", string("key", "k2"), string("value", "v2"))
            )
        )

        assertEquals(listOf("attrs.[k1]" to "v1", "attrs.[k2]" to "v2"), RedisHashFlattener.flatten("attrs", gene))
    }

    @Test
    fun testMapOfObjectsIsStoredWithKeyAndDottedPath() {
        val template = PairGene("template", string("key", ""), address("value", "", 0))
        val gene = FixedMapGene(
            "byCity", template,
            elements = mutableListOf(PairGene("e1", string("key", "R"), address("value", "Rosario", 1)))
        )

        assertEquals(
            listOf("byCity.[R].city" to "Rosario", "byCity.[R].zip" to "1"),
            RedisHashFlattener.flatten("byCity", gene)
        )
    }

    @Test
    fun testObjectContainingAListOfObjectsComposesPaths() {
        val addrs = ArrayGene("addrs", address("addrs", "", 0), elements = mutableListOf(address("addrs", "X", 1)))
        val gene = ObjectGene("person", listOf(OptionalGene("addrs", addrs)))

        assertEquals(
            listOf("person.addrs.[0].city" to "X", "person.addrs.[0].zip" to "1"),
            RedisHashFlattener.flatten("person", gene)
        )
    }

    @Test
    fun testQueryFieldWithConstantIsASingleEntryNamedAfterIt() {
        assertEquals(listOf("id" to "42"), RedisQueryField("id", constantValue = "42").hashEntries())
    }

    @Test
    fun testQueryFieldWithGeneIsFlattenedUnderItsName() {
        val field = RedisQueryField("address", valueGene = address("address", "Rosario", 2000))

        assertEquals(listOf("address.city" to "Rosario", "address.zip" to "2000"), field.hashEntries())
    }
}
