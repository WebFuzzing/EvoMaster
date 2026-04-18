package org.evomaster.core.problem.rest.arazzo.models

import org.evomaster.core.problem.rest.arazzo.models.commons.ArazzoSpecificationsCommon

class ArazzoSpecifications(
    common: ArazzoSpecificationsCommon,
    val workflows: List<Workflow>,
) : ArazzoSpecificationsCommon by common