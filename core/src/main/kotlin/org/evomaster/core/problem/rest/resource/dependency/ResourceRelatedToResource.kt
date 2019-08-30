package org.evomaster.core.problem.rest.resource.dependency

import org.evomaster.core.problem.rest.RestPath

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

    open fun getDependentResources(target : String, exceptDirectHierarchy : Boolean = true, exclude : List<String> = mutableListOf()) : List<String>{
        if (!path.contains(target))
            throw IllegalArgumentException("$target does not belong to this ResourceRelatedToResources")
        val rpath = RestPath(target)
        return targets.filter { !exclude.contains(it) && (!exceptDirectHierarchy || !RestPath(it).run { rpath.isDirectChildOf(this) || rpath.isAncestorOf(this)})}
    }

    open fun toCSVHeader() = listOf(
            "type", "source", "targets", "probability", "additionInfo", "others"
    )

    open fun toCSV() : List<String> = listOf(
            this::class.java.simpleName, path.joinToString(getSeparator()), targets.joinToString(getSeparator()), probability.toString(), additionalInfo, ""
    )

    open fun exportCSV() : List<String>{
        if (toCSVHeader().size != toCSV().size)
            throw IllegalArgumentException("inconsistent header and content")
        return toCSV()
    }

    open fun getSeparator() = ";"
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

    override fun toCSV() : List<String> = listOf(
            this::class.java.simpleName, path.joinToString(getSeparator()), targets.joinToString(getSeparator()), probability.toString(), additionalInfo, referredTables.joinToString(getSeparator())
    )
}

class SelfResourcesRelation(path : String, probability: Double = 1.0, info: String = "") : ResourceRelatedToResources(mutableListOf(path), mutableListOf(path), probability, info)