package org.evomaster.client.java.controller.api.dto.database.execution.epa

class RestAction(
    var verb: String,
    var path: String
) {
    override fun toString(): String {
        return "$path::$verb"
    }
}
