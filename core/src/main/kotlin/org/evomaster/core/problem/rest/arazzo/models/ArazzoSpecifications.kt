package org.evomaster.core.problem.rest.arazzo.models

class ArazzoSpecifications(
    val arazzo: String,
    val info: InfoArazzo,
    val sourceDescriptions: MutableList<SourceDescription>,
    val workflows: MutableList<Workflow>,
    val components: Components?
) {

}