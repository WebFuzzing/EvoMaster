package org.evomaster.core.search.service


/**
 * To represent and identify a coverage target, we use numeric ids.
 * But those are not very "descriptive" of what the targets actually are.
 * So, we need a mapping to a String description (which itself would be
 * a unique id).
 * Note: we do not use these strings directly as it would be too inefficient.
 * Furthermore, as the ids are passed as query parameters in HTTP GET requests,
 * there are limits on length
 */
class IdMapper {

    private val mapping : MutableMap<Int, String> = mutableMapOf()


    fun addMapping(id: Int, descriptiveId: String){
        mapping[id] = descriptiveId
    }

    fun getDescriptiveId(id: Int) = mapping[id] ?: "undefined"
}