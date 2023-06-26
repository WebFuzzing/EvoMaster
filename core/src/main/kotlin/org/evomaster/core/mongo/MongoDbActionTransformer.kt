package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.core.search.gene.utils.GeneUtils

object MongoDbActionTransformer {

    fun transform(actions: List<MongoDbAction>) : MongoDatabaseCommandDto {

        val insertionDtos = mutableListOf<MongoInsertionDto>()

        for (action in actions) {
            val genes = action.seeTopGenes().first()

            val insertionDto = MongoInsertionDto().apply {
                databaseName = action.database
                collectionName = action.collection
                data = genes.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
            }

            insertionDtos.add(insertionDto)
        }

        return MongoDatabaseCommandDto().apply { this.insertions = insertionDtos }
    }
}