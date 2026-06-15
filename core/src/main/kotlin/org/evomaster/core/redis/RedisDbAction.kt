package org.evomaster.core.redis

import org.evomaster.core.search.action.EnvironmentAction

/**
 * Represents an action to insert data into a Redis database, generated in response
 * to a failed Redis read command.
 */
sealed class RedisDbAction : EnvironmentAction(listOf()) {

    /**
     * Returns the primary key this action targets, if any.
     * Commands that operate on patterns rather than specific keys (e.g., KEYS)
     * return null.
     */
    open fun getTargetKey(): String? = null

    open fun insertionsCount(): Int = 1

    override fun getActionGroupKey(): String = RedisDbAction::class.java.name
}