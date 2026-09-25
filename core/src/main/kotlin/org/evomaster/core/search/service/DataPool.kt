package org.evomaster.core.search.service

import com.google.inject.Inject
import opennlp.tools.stemmer.PorterStemmer
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.IdHeuristics
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.RestResponseFeeder
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import java.util.Deque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Service to keep track of data values associated with a string key.
 * The key can be used to define links between actions, to enable smart re-use of data.
 * Keys are case insensitive and stemmed.
 *
 * Consider the REST example of:
 *
 * (1) GET     /users
 * (2) DELETE  /users/{id}
 *
 * The data regarding ids fetched in (1) could be associated with the key "userid".
 * Such data can then be read and reuse to create the id param in (2).
 *
 * In this specific context, this approach is also called "Response Dictionary".
 *
 * For the time being, we support 2 types of values: numeric and string.
 *
 * Matching a key does not need to be 100%... some level of differences can be tolerated.
 * We follow a similar algorithm as described in Section 5.2.1 of:
 *
 * "Automated black-box testing of nominal and error scenarios in RESTful APIs"
 *
 * However, there are several different kinds of sources for data, not just reponses.
 * And those can be handled separately, with different priorities
 */
class DataPool() {

    private enum class DPType{
        RESPONSES,
        EXAMPLES,
        DICTIONARY,
        SUCCESSES
    }

    private data class DataPoolInstance(
        val type: DPType,
        val data: MutableMap<String, Deque<String>>,
        val priority: Int
    )

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var randomness: Randomness


    private val pools = mapOf<DPType, DataPoolInstance> (
        DPType.RESPONSES to  DataPoolInstance(DPType.RESPONSES, ConcurrentHashMap(), 4),
        DPType.SUCCESSES to  DataPoolInstance(DPType.SUCCESSES, ConcurrentHashMap(), 3),
        DPType.EXAMPLES to  DataPoolInstance(DPType.EXAMPLES, ConcurrentHashMap(), 2),
        DPType.DICTIONARY to  DataPoolInstance(DPType.DICTIONARY, ConcurrentHashMap(), 1),
    )

    internal constructor(_config: EMConfig, _randomness: Randomness) : this(){
        config = _config
        randomness = _randomness
    }

    fun keySize() = pools.values.sumOf{it.data.size}

    fun normalize(s: String) : String{

        return stem(s.lowercase())
    }

    private fun stem(s: String) : String{
        return PorterStemmer().stem(s)
    }

    fun applyTo(gene: Gene) : Boolean{
        val context =
            //are we inside an object? if so, take object's name
            gene.getFirstParent(ObjectGene::class.java)?.name
            //alternative, are we inside a path element? if so, resolve name from full path
                ?: gene.getFirstParent(PathParam::class.java)?.let {
                    gene.getFirstParent(RestCallAction::class.java)?.path?.nameQualifier
                }

        val x = extractValue(gene.name, context)
            ?: return false

        if(IdHeuristics.heuristicIsId(gene.name) && gene.getFirstParent(PathParam::class.java)!=null){
            val action = gene.getFirstParent(RestCallAction::class.java)
            if(action != null && action.verb.isWriteOperation()){
                /*
                    if we are in path param which potentially represents an id, and the current
                    action is a write-one, then do not apply data pool.
                    The reason is that we want to avoid manipulating possibly existing resources.
                    Those should rather be handled with smart seeding with action dependencies
                    among HTTP calls
                 */
                return false
            }
        }

        val applied = gene.setFromStringValue(x)
        return applied
    }

    fun addValueFromResponses(key: String, data: String){
        addValue(key, data, DPType.RESPONSES)
    }

    fun addValueFromExamples(key: String, data: String){
        addValue(key, data, DPType.EXAMPLES)
    }

    fun addValueFromDictionary(key: String, data: String){
        addValue(key, data, DPType.DICTIONARY)
    }

    fun addValueFromSuccesses(key: String, data: String){
        addValue(key, data, DPType.SUCCESSES)
    }


    private fun addValue(key: String, data: String, type: DPType) {
        addValue(key, data, pools[type]!!.data)
    }

    private fun addValue(key: String, data: String, pool: MutableMap<String, Deque<String>>){

        synchronized(pool) {
            val queue = pool.getOrPut(normalize(key)) { ConcurrentLinkedDeque() }

            if (queue.contains(data)) {
                return // already there
            }

            if (queue.size == config.maxSizeDataPool) {
                queue.removeFirst()
            }
            queue.addLast(data)
        }
    }

    /**
     * Mainly for testing
     */
    fun hasExactKey(key: String) = pools.any{it.value.data.containsKey(key)}

    /**
     * Mainly for testing
     */
    fun extractAllWithExactKey(key: String) = pools.values.flatMap {  it.data[key]?.toList() ?: listOf()}

    /**
     * Extract a value from the pool, given the input key.
     * The key matching does not need to be exact, as some minor level of mismatches are tolerated.
     *
     * @param [objectName] qualifier name of the object context, if any. Mainly needed to deal with "id" keys.
     */
    fun extractValue(key: String, objectName: String? = null) : String?{

        //We synchronized insertions, as that might lead to inconsistencies... but read operation should be (hopefully)
        //fine, concurrent as anyway the pool is
        //synchronized(pool)

        val available = pools.filter { it.value.data.isNotEmpty() }.map { it.value }.toMutableList()

        if(available.isEmpty()){
            return null
        }

        val k = normalize(key) // eg "Pets" get converted into "pet"

        /*
            if more than a pool is available, choose with highest priority.
            however, at times, we should choose at random, as we don't keep track (yet)
            how pools have been previously used (eg, avoid starvation)
        */
        if(randomness.nextBoolean()){ // 50% chances
            randomness.shuffle(available)
        } else {
            available.sortBy { -it.priority }
        }

        for(pool in available){
            val res = extractFromPool(pool.data, k, objectName)
            if(res != null){
                return res
            }
        }
        return null
    }

    private fun extractFromPool(pool: Map<String, Deque<String>>,k: String, objectName: String?): String? {
        //(1) first exact match
        var data = pool[k]
        if (data != null) {
            return randomness.choose(data)
        }

        //(2) check exact match with object qualifier
        val fullQualifier = fullQualifier(k, objectName)
        if (fullQualifier != null) {
            data = pool[fullQualifier]
            if (data != null) {
                return randomness.choose(data)
            }
        }

        //(3) partial match
        val closestKey = closestKey(k, pool)
        if (closestKey != null) {
            return randomness.choose(pool[closestKey]!!)
        }

        //(4) partial match with object qualifier
        if (fullQualifier != null) {
            val closestFullQualifier = closestKey(fullQualifier, pool)
            if (closestFullQualifier != null) {
                return randomness.choose(pool[closestFullQualifier]!!)
            }
        }

        //(5) check if any key is a substring
        val sub = pool.keys.firstOrNull { k.contains(it, true) }
        if (sub != null) {
            return randomness.choose(pool[sub]!!)
        }

        //got nothing
        return null;
    }

    private fun fullQualifier(normalizedK : String, objectName: String?) : String?{
        if(objectName == null){
            return null
        }
        val name = normalize(objectName) // eg "Users" into "user"
        val id = name + normalizedK  // eg "userpet"
        return id
    }

    private fun closestKey(k: String, pool: Map<String, Deque<String>>): String? {

        val distance = org.apache.commons.text.similarity.LevenshteinDistance(config.thresholdDistanceForDataPool)

        val closest = pool.keys
            .map { Pair(it, distance.apply(it, k)) }
            .filter { it.second >= 0 }
            .minByOrNull { it.second }
            ?.first
        return closest
    }


}