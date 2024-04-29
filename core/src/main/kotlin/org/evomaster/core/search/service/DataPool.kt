package org.evomaster.core.search.service

/**
 * Service to keep track of data values associated with a string key.
 * The key can be used to define links between actions, to enable smart re-use of data.
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

    private val stringData : MutableMap<String,String> = mutableMapOf()

    //private val
}