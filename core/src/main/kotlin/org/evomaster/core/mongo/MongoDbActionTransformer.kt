package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.operations.*

object MongoDbActionTransformer {

    fun transform(insertions: List<MongoDbAction>) : MongoDatabaseCommandDto {

        val list = mutableListOf<MongoInsertionDto>()

        for (i in 0 until insertions.size) {

            val action = insertions[i]

            val insertion = MongoInsertionDto().apply { collectionName = action.collection }

            val g = action.seeTopGenes().first()
            val entry = MongoInsertionEntryDto()

            // If is printed as JSON there might a problem
            // A Document(from Mongo) can be created from a JSON but some info might be lost
            // Preferably is created from a EJSON (Extended JSON) as JSON can only directly represent a
            // subset of the types supported by BSON.
            // Maybe we can create a new OutputFormat

            entry.value = g.getValueAsPrintableString()
            entry.fieldName = g.getVariableName()

            insertion.data.add(entry)

            list.add(insertion)
        }

        return MongoDatabaseCommandDto().apply { this.insertions = list }
    }
}