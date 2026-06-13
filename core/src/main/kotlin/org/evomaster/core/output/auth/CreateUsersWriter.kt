package org.evomaster.core.output.auth

import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.search.Individual

object CreateUsersWriter {

    fun getCreateUsersAuth(ind: Individual) = ind.seeAllActions()
        .filterIsInstance<HttpWsAction>()
        .filter { it.auth.createUsers != null }
        .distinctBy { it.auth.name }
        .map { it.auth.createUsers!! }
}