package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions

data class Vertex(val isInitial: Boolean, val index: Int, val enabledEndpoints: RestActions)