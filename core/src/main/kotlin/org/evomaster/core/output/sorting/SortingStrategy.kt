package org.evomaster.core.output.sorting

enum class SortingStrategy {

    COVERED_TARGETS,
    TARGET_INCREMENTAL
    ;

    fun isCoveredTargets() = this.name.equals("covered_targets", true)
    fun isTargetIncremental() = this.name.equals("target_incremental", true)
}
