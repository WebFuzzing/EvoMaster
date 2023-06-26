package org.evomaster.core.mongo

import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import java.lang.reflect.Modifier
import java.util.*

class MongoDbAction(
    /**
     * The database to insert document into
     */
    val database: String,
    /**
     * The collection to insert document into
     */
    val collection: String,
    /**
     * The type of the new document. Should map the type of the documents of the collection
     */
    val documentsType: Class<*>,
    computedGenes: List<Gene>? = null
) : Action(listOf()) {

    private val genes: List<Gene> = (computedGenes ?: computeGenes()).also { addChildren(it) }

    private fun computeGenes(): List<Gene> {
        val genes =
            if (documentsType.name == "org.bson.Document") {
                listOf()
            } else {
                getFieldsFromType().mapNotNull { MongoActionGeneBuilder().buildGene(it.name, it.type) }
            }
        return Collections.singletonList(ObjectGene("BSON", genes))
    }

    override fun getName(): String {
        return "MONGO_Insert_${database}_${collection}_${
            getFieldsFromType().map { it.name }.sorted().joinToString("_")
        }"
    }

    override fun seeTopGenes(): List<out Gene> {
        return genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun copyContent(): Action {
        return MongoDbAction(database, collection, documentsType, genes.map(Gene::copy))
    }

    private fun getFieldsFromType() = documentsType.declaredFields.filter { Modifier.isPublic(it.modifiers) }
}