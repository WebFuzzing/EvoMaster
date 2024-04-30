package org.evomaster.core.search.service

import com.google.inject.Inject
import opennlp.tools.stemmer.PorterStemmer
import org.evomaster.core.EMConfig

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
 *
 */
class DataPool {

    @Inject
    lateinit var config: EMConfig
        private set

    @Inject
    lateinit var randomness: Randomness
        private set

    private val stringData : MutableMap<String, ArrayDeque<String>> = mutableMapOf()

    private val stemmer = PorterStemmer()

    fun normalize(s: String) : String{

        return stem(s.lowercase())
    }

    private fun stem(s: String) : String{
        stemmer.reset()
        return stemmer.stem(s)
    }

    fun addValue(key: String, data: String){

        val queue = stringData.getOrPut(normalize(key)) { ArrayDeque<String>() }

        if(queue.contains(data)){
            return // already there
        }

        if(queue.size == config.maxSizeDataPool){
            queue.removeFirst()
        }
        queue.addLast(data)
    }


    fun stringValue(key: String, objectName: String? = null) : String?{

        val k = normalize(key) // eg "Pets" get converted into "pet"

        //first exact match
        var data = stringData[k]
        if(data != null){
            return randomness.choose(data)
        }

        if(objectName != null){
            val name = normalize(objectName) // eg "Users" into "user"
            val id = name + k  // eg "userpet"

            data = stringData[id]
            if(data != null){
                return randomness.choose(data)
            }
        }
    }


}