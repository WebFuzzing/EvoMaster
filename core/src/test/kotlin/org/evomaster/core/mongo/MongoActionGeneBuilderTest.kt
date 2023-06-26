package org.evomaster.core.mongo

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Test
import java.util.*

class MongoActionGeneBuilderTest {

    inner class Object {
        var someField: Int = 0
    }

    @Test
    fun testStringField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", String::class.java)
        assert(StringGene::class.isInstance(gene))
    }

    @Test
    fun testIntegerField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Integer::class.java)
        assert(IntegerGene::class.isInstance(gene))
    }

    @Test
    fun testLongField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Long::class.java)
        assert(LongGene::class.isInstance(gene))
    }

    @Test
    fun testDoubleField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Double::class.java)
        assert(DoubleGene::class.isInstance(gene))
    }

    @Test
    fun testDateField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Date::class.java)
        assert(DateGene::class.isInstance(gene))
    }

    @Test
    fun testBooleanField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Boolean::class.java)
        assert(BooleanGene::class.isInstance(gene))
    }

    @Test
    fun testObjectField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Object::class.java)
        assert(ObjectGene::class.isInstance(gene))
    }

    @Test
    fun testUnhandledTypeField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Class.forName("org.bson.types.Decimal128"))
        assert(gene == null)
    }

    @Test
    fun testDocumentField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", Class.forName("org.bson.Document"))
        assert(ObjectGene::class.isInstance(gene))
    }

    /*
    @Test
    fun testListField() {
        val gene = MongoActionGeneBuilder().buildGene("someField", ArrayList<String>()::class.java)
        assert(ArrayGene::class.isInstance(gene))
    }

     */
}