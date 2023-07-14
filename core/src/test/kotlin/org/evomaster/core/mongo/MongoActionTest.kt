package org.evomaster.core.mongo

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.junit.jupiter.api.Test

class MongoActionTest {
    @Test
    fun testGenes() {
        val action = MongoDbAction(
            "someDatabase",
            "someCollection",
            "\"CustomType\":{\"CustomType\":{\"type\":\"object\", \"properties\": {\"aField\":{\"type\":\"integer\"}}, \"required\": [\"aField\"]}}"
        )
        val gene = action.seeTopGenes().first()
        assert(ObjectGene::class.isInstance(gene))
        gene as ObjectGene
        assert(IntegerGene::class.isInstance(gene.fields.first()))
    }
}