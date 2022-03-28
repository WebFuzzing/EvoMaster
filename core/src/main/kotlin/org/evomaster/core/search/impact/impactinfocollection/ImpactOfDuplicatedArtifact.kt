package org.evomaster.core.search.impact.impactinfocollection

/**
 * created by manzh on 2020-04-08
 *
 * this is to extract Duplicated group, e.g., abc-abc-abc, particularly designed for SQL initialization actions
 */
abstract class ImpactOfDuplicatedArtifact<T> (
        /**
         * complete impacts info for each of element T
         */
        val completeSequence : MutableList<T> = mutableListOf(),
        /**
         * key is name of template
         * value is a list of impacts for the template
         */
        val template: MutableMap<String, List<T>> = mutableMapOf(),

        /**
         * key is name of template
         * value is an impact on duplicate times of the template
         */
        val templateDuplicateTimes: MutableMap<String, Impact> = mutableMapOf()
)