package org.evomaster.core.search

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult


open class EvaluatedAction(open val action: Action, open val result: ActionResult)

/**
 * specialized evaluated db action
 */
class EvaluatedDbAction(override val action: DbAction, override val result: DbActionResult) : EvaluatedAction(action, result)