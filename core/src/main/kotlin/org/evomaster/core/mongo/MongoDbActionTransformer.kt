package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.operations.*

object MongoDbActionTransformer {

    fun transform(insertions: List<MongoDbAction>) : MongoDatabaseCommandDto {

        val list = mutableListOf<MongoInsertionDto>()

        for (element in insertions) {

            val insertion = MongoInsertionDto().apply {
                databaseName = element.database
                collectionName = element.collection }

            val g = element.seeTopGenes().first()
            val entry = MongoInsertionEntryDto()

            // If is printed as JSON there might a problem
            // A Document(from Mongo) can be created from a JSON but some info might be lost
            // Preferably is created from an EJSON (Extended JSON) as JSON can only directly represent a
            // subset of the types supported by BSON.
            // Maybe we can create a new OutputFormat

            entry.value = g.getValueAsPrintableString()

            // This is ignored for now
            entry.fieldName = g.getVariableName()

            insertion.data.add(entry)

            list.add(insertion)
        }

        return MongoDatabaseCommandDto().apply { this.insertions = list }
    }
}