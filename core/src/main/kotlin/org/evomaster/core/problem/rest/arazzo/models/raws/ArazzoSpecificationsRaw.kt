package org.evomaster.core.problem.rest.arazzo.models.raws

import org.evomaster.core.problem.rest.arazzo.models.Components
import org.evomaster.core.problem.rest.arazzo.models.InfoArazzo
import org.evomaster.core.problem.rest.arazzo.models.SourceDescription
import org.evomaster.core.problem.rest.arazzo.models.commons.ArazzoSpecificationsCommon

class ArazzoSpecificationsRaw(
    override val arazzo: String,
    override val info: InfoArazzo,
    override val sourceDescriptions: List<SourceDescription>,
    override val components: Components?,
    val workflows: List<WorkflowRaw>
) : ArazzoSpecificationsCommon