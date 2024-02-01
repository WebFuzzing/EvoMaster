package org.evomaster.client.java.controller.api.dto.database.execution.epa

import kotlin.collections.HashSet

class RestActions (
    var enabledRestActions: HashSet<RestAction> = HashSet()
) {
    fun toStringForEPA(): String {
        return enabledRestActions.stream()
            .map { obj: RestAction -> obj.toString() }
            .sorted()
            .reduce { s: String?, a: String? -> "$s, \\n$a" }
            .orElse("")
    }

    companion object {
        fun from(restActionsDto: RestActionsDto) : RestActions {
            val set = restActionsDto.enabledRestActions.stream().map { RestAction(it.verb, it.path) }.toArray().toHashSet() as HashSet<RestAction>
            return RestActions(set)
        }
    }
}