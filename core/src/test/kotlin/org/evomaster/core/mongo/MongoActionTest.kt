package org.evomaster.core.mongo

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.junit.jupiter.api.Test

class MongoActionTest {

    inner class Object {
        @JvmField
        var someField: Int = 0
    }

    @Test
    fun testGenesWhenDocument() {
        val action = MongoDbAction("someDatabase", "someCollection", Class.forName("org.bson.Document"))
        val gene = action.seeTopGenes().first()
        assert(ObjectGene::class.isInstance(gene))
        gene as ObjectGene
        assert(gene.fields.isEmpty())
    }

    @Test
    fun testGenesWhenNotDocument() {
        val action = MongoDbAction("someDatabase", "someCollection", Object::class.java)
        val gene = action.seeTopGenes().first()
        assert(ObjectGene::class.isInstance(gene))
        gene as ObjectGene
        assert(IntegerGene::class.isInstance(gene.fields.first()))
    }
}