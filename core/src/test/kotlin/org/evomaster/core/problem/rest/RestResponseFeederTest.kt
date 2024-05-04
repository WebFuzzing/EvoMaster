package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.service.DataPool
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.ws.rs.core.MediaType

class RestResponseFeederTest{

    private var pool = createPool()


    private fun createPool(threshold: Int? = null) : DataPool {
        val config = EMConfig()
        if(threshold!=null){
            config.thresholdDistanceForDataPool = threshold
        }
        return DataPool(config, Randomness())
    }

    @BeforeEach
    fun initTest(){
        pool = createPool()
    }

    private fun createActionWithResponse(
        verb: HttpVerb,
        path: String,
        status: Int,
        payload: String
    ) : EvaluatedAction{

        val action = RestCallAction("42",verb,RestPath(path), mutableListOf())
        action.setLocalId("foo")

        val response = RestCallResult(action.getLocalId(), false)
        response.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        response.setBody(payload)
        response.setStatusCode(status)

        return  EvaluatedAction(action, response)
    }

    private fun feed(verb: HttpVerb,
                     path: String,
                     status: Int,
                     payload: String){

        val ea = createActionWithResponse(verb, path, status, payload)
        RestResponseFeeder.handleResponse(ea.action as RestCallAction, ea.result as RestCallResult, pool)
    }


    @ParameterizedTest
    @ValueSource(ints = [100, 300, 302, 400, 401, 403, 404, 500, 503])
    fun testWrongCode(status: Int){
        assertEquals(0, pool.keySize())
        feed(HttpVerb.GET, "/foo", status, "42")
        assertEquals(0, pool.keySize())
    }

    @ParameterizedTest
    @ValueSource(strings = ["PUT","PATCH","DELETE","HEAD"])
    fun testWrongCode(verb: String){
        assertEquals(0, pool.keySize())
        feed(HttpVerb.valueOf(verb), "/foo", 200, "42")
        assertEquals(0, pool.keySize())
    }


    @Test
    fun testString(){
        assertEquals(0, pool.keySize())

        val name = "foo"
        feed(HttpVerb.GET, "/name", 200, "\"$name\"")

        assertEquals(1, pool.keySize())
        val res = pool.extractValue("name")
        assertEquals(name, res)
    }

    @Test
    fun testNumber(){
        assertEquals(0, pool.keySize())

        val x = "42"
        feed(HttpVerb.GET, "/api/v3/balance", 200, "$x")

        assertEquals(1, pool.keySize())
        val res = pool.extractValue("balance")
        assertEquals(x, res)
    }

    @Test
    fun testBasicObject(){
        assertEquals(0, pool.keySize())

        val name = "foo"
        val surname = "bar"
        val age = 42
        val payload = """
            {
                "name": "$name",
                "surname": "$surname",
                "age": $age
            }
        """.trimIndent()
        feed(HttpVerb.GET, "/api/v3/balance", 200, payload)

        assertEquals(3, pool.keySize())
        assertEquals(name, pool.extractValue("name"))
        assertEquals(surname, pool.extractValue("surnames"))
        assertEquals(age, pool.extractValue("ag")!!.toInt())
    }

    /*
        TODO
        - nested objects
        - ids
        - arrays
        - POST
     */
}
