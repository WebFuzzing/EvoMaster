package org.evomaster.core.problem.rest.arazzo.models

class ArazzoSpecifications(
    val arazzo: String,
    val info: InfoArazzo,
    val sourceDescriptions: List<SourceDescription>,
    val workflows: MutableList<Workflow>,
    val components: Components?
) {

}