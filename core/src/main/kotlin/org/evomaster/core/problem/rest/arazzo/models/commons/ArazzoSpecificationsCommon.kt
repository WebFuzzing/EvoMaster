package org.evomaster.core.problem.rest.arazzo.models.commons

import org.evomaster.core.problem.rest.arazzo.models.Components
import org.evomaster.core.problem.rest.arazzo.models.InfoArazzo
import org.evomaster.core.problem.rest.arazzo.models.SourceDescription

interface ArazzoSpecificationsCommon {
    val arazzo: String
    val info: InfoArazzo
    val sourceDescriptions: List<SourceDescription>
    val components: Components?
}