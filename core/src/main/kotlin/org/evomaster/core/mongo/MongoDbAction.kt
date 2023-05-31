package org.evomaster.core.mongo

import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import java.util.*

class MongoDbAction(val database: String, val collection: String, val documentsType: Class<*>, val accessedFields: Map<String, Any>, computedGenes: List<Gene>? = null) : Action(listOf()) {

    val genes: List<Gene> = (computedGenes ?: computeGenes()) .also { addChildren(it) }

    private fun computeGenes(): List<Gene> {
        // I should be as specific as I can with the type the collection's documents should have.
        // There are a few ways to get that info:

        // 1) Using <documentsType>, which is the result of collection.getDocumentsClass()
        //    Spring for example "ignore" this and store type info inside SampleMongoRepository

        // 2) Instrument the constructor of SampleMongoRepository and retrieve the info
        //    Probably can reuse something from GsonClassReplacement.

        // 3) Extract from the query the fields (<accessedFields>) used and type of each of them. This probably won't
        //    work fine as usually a subset of fields is used in a query. But is better than creating a
        //    Document.

        val genes =
        if(documentsType.typeName == "org.bson.Document"){
            // 3)
            accessedFields.map { MongoActionGeneBuilder().buildGene(it.key, it.value) }
        }else{
            // 1)
            documentsType.declaredFields.map { MongoActionGeneBuilder().buildGene(it.name, it.type)  }
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