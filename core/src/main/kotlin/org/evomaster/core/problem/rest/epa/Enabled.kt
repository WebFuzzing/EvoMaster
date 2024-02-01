package org.evomaster.core.problem.rest.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions


data class Enabled(
    var associatedRestAction: RestAction,
    var enabledRestActions: RestActions = RestActions()
)