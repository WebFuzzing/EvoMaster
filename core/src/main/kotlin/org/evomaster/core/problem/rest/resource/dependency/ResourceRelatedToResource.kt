package org.evomaster.core.problem.rest.resource.dependency

/**
 *  @param path is a list of path(s), which can be parsed with [RelatedTo.parseMultipleKey]
 *  @param target is a list of paths of related rest resources
 */
open class ResourceRelatedToResources(
        val path : List<String>,
        override val targets: MutableList<String>,
        probability: Double = 1.0,
        info: String = ""
) : RelatedTo(generateKeyForMultiple(path), targets, probability, info){

    init {
        assert(path.isNotEmpty())
    }

}
/**
 * this class presents mutual relations among resources that are derived based on tables.
 *
 * @param info provides information that supports the resources are mutual relations of each other,
 *          e.g., the resources are related to same table, and set [info] name of the table.
 *
 *          Note that related table might be derived based on token parser, not confirmed regarding evomaster driver.
 *          [confirmedSet] is used to represent whether the mutual relation is confirmed.
 */
class MutualResourcesRelations(mutualResources: List<String>, probability: Double, var referredTables : MutableSet<String> = mutableSetOf())
    : ResourceRelatedToResources(mutualResources, mutualResources.toMutableList(), probability, ""){

    override fun getName(): String {
        return "MutualRelations:${notateKey()}"
    }
}

class SelfResourcesRelation(path : String, probability: Double = 1.0, info: String = "") : ResourceRelatedToResources(mutableListOf(path), mutableListOf(path), probability, info)