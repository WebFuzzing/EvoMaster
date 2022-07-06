package org.evomaster.core.search

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.external.service.ExternalServiceAction

class InitializationActions {

    private val dbInitializationAction: MutableList<DbAction>
    get() {
        return dbInitializationAction
    }

    private val externalServiceAction: MutableList<ExternalServiceAction>
    get() {
        return externalServiceAction
    }
    
    fun getInitializingActions() : List<Action> {
        return listOf(dbInitializationAction).plus(externalServiceAction) as List<Action>
    }

}