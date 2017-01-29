package org.evomaster.core.search.service

import java.util.concurrent.atomic.AtomicInteger


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

    private val reverseMapping : MutableMap<String, Int> = mutableMapOf()

    /**
     * Counter used to create local id, based on the return values
     * of the interactions with the SUT.
     * The counter has to be negative to avoid collisions with
     * ids generated on the SUT (eg, via bytecode instrumentation
     * monitoring)
     */
    private val localCounter = AtomicInteger(-1)

    fun addMapping(id: Int, descriptiveId: String){
        mapping[id] = descriptiveId
        reverseMapping[descriptiveId] = id
    }

    fun getDescriptiveId(id: Int) = mapping[id] ?: "undefined"

    fun handleLocalTarget(descriptiveId: String) : Int{
        return reverseMapping.getOrPut(descriptiveId, {
            val k = localCounter.decrementAndGet()
            mapping[k] = descriptiveId
            k
        })
    }
}