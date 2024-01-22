package org.evomaster.core.search

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.mongo.MongoDbActionResult


open class EvaluatedAction(open val action: Action, open val result: ActionResult){
    init{
        if(action.getLocalId() != result.sourceLocalId){
            throw IllegalArgumentException("Mismatch between action local id ${action.getLocalId()} and" +
                    " the source id ${result.sourceLocalId} in the associated result")
        }
    }
}


/**
 * specialized evaluated db action
 */
class EvaluatedDbAction(override val action: SqlAction, override val result: SqlActionResult) : EvaluatedAction(action, result)

class EvaluatedMongoDbAction(override val action: MongoDbAction, override val result: MongoDbActionResult) : EvaluatedAction(action, result)