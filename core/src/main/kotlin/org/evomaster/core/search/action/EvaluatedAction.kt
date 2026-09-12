package org.evomaster.core.search.action

import org.evomaster.core.database.cassandra.CassandraDbAction
import org.evomaster.core.database.cassandra.CassandraDbActionResult
import org.evomaster.core.database.mongo.MongoDbAction
import org.evomaster.core.database.mongo.MongoDbActionResult
import org.evomaster.core.database.redis.RedisDbAction
import org.evomaster.core.database.redis.RedisDbActionResult
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.database.sql.SqlActionResult


open class EvaluatedAction(val action: Action, val result: ActionResult){
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
class EvaluatedDbAction(val sqlAction: SqlAction, val sqlResult: SqlActionResult) : EvaluatedAction(sqlAction, sqlResult)

class EvaluatedMongoDbAction(val mongoAction: MongoDbAction, val mongoResult: MongoDbActionResult) : EvaluatedAction(mongoAction, mongoResult)

class EvaluatedRedisDbAction(val redisAction: RedisDbAction, val redisResult: RedisDbActionResult) : EvaluatedAction(redisAction, redisResult)

class EvaluatedCassandraDbAction(val cassandraAction: CassandraDbAction, val cassandraResult: CassandraDbActionResult) : EvaluatedAction(cassandraAction, cassandraResult)