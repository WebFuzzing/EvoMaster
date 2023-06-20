package org.evomaster.core.mongo

import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import java.util.*

class MongoDbAction(
    val database: String,
    val collection: String,
    val documentsType: Class<*>,
    val accessedFields: Map<String, Class<*>>,
    computedGenes: List<Gene>? = null
) : Action(listOf()) {

    val genes: List<Gene> = (computedGenes ?: computeGenes()).also { addChildren(it) }

    private fun computeGenes(): List<Gene> {
        val genes =
            if (documentsType.typeName == "org.bson.Document") {
                accessedFields.map { MongoActionGeneBuilder().buildGene(it.key, it.value) }
            } else {
                documentsType.declaredFields.map { MongoActionGeneBuilder().buildGene(it.name, it.type) }
            }
        return Collections.singletonList(ObjectGene("BSON", genes))
    }

    override fun getName(): String {
        return "MONGO_Find_${collection}_${accessedFields.map { it.key }.sorted().joinToString("_")}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun copyContent(): Action {
        return MongoDbAction(database, collection, documentsType, accessedFields, genes.map(Gene::copy))
    }
}