package org.evomaster.core.mongo

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.mongo.ObjectIdGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun testDocumentGenes() {
        val action = MongoDbAction(
            "annotator",
            "ensembl.canonical_transcript_per_hgnc",
            "\"org.bson.Document\":{\"org.bson.Document\":{\"type\":\"object\", \"additionalProperties\":{\"type\":\"string\"}}}"
        )
        val gene = action.seeTopGenes().first()
        assert(ObjectGene::class.isInstance(gene))
        gene as ObjectGene
        assertEquals(1, gene.fields.size)
        assert(ObjectIdGene::class.isInstance(gene.fields.first()))
    }

    @Test
    fun testDocumentWithOptionalFieldGene() {
        val action = MongoDbAction(
            "annotator",
            "ensembl.canonical_transcript_per_hgnc",
            "\"org.bson.Document\":{\"org.bson.Document\":{\"type\":\"object\", \"properties\": {\"aField\":{\"type\":\"integer\"}}, \"additionalProperties\":{\"type\":\"string\"}}}"
        )
        val gene = action.seeTopGenes().first()
        assert(ObjectGene::class.isInstance(gene))
        gene as ObjectGene
        assertEquals(2, gene.fields.size)
        assert(OptionalGene::class.isInstance(gene.fields[0]))
        assert(ObjectIdGene::class.isInstance(gene.fields[1]))
    }

    @Test
    fun testDocumentWithRequiredFieldGene() {
        val action = MongoDbAction(
            "annotator",
            "ensembl.canonical_transcript_per_hgnc",
            "\"org.bson.Document\":{\"org.bson.Document\":{\"type\":\"object\", \"properties\": {\"aField\":{\"type\":\"integer\"}}, \"required\": [\"aField\"], \"additionalProperties\":{\"type\":\"string\"}}}"
        )
        val gene = action.seeTopGenes().first()
        assert(ObjectGene::class.isInstance(gene))
        gene as ObjectGene
        assertEquals(2, gene.fields.size)
        assert(IntegerGene::class.isInstance(gene.fields[0]))
        assert(ObjectIdGene::class.isInstance(gene.fields[1]))
    }
}