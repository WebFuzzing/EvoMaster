package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.action.EvaluatedAction
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
        assertEquals(0, pool.keySize())
    }

    private fun createActionWithResponse(
        verb: HttpVerb,
        path: String,
        status: Int,
        payload: String
    ) : EvaluatedAction {

        val action = RestCallAction("42",verb, RestPath(path), mutableListOf())
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


    @Test
    fun testNestedObject(){

        val name = "foo"
        val surname = "bar"
        val city = "Oslo"
        val country = "Norway"
        val payload = """
            {
                "name": "$name",
                "surname": "$surname",
                "address": {
                    "city" : "$city",                   
                    "country": "$country"                                       
                }
            }
        """.trimIndent()
        feed(HttpVerb.GET, "/api/users/{id}", 200, payload)
        assertEquals(4, pool.keySize())
        assertEquals(name, pool.extractValue("name"))
        assertEquals(surname, pool.extractValue("surnames"))
        assertEquals(city, pool.extractValue("city"))
        assertEquals(country, pool.extractValue("country"))
    }

    @Test
    fun testIdsBase(){

        val id = "123456"
        val payload = """
            {
                "id": $id,
                "name": "foo",
                "surname": "bar"
            }
        """.trimIndent()

        feed(HttpVerb.GET, "/api/users/{id}", 200, payload)
        assertEquals(id, pool.extractValue("userid"))
        assertTrue(pool.hasExactKey("userid"))
    }


    @Test
    fun testIdsComplex(){

        val id = "dslfmlefm"
        val f1 = "fdsfegfdddddd"
        val f2 = "2223rdsdsc"
        val payload = """
            {
                "id": "$id",
                "name": "foo",
                "surname": "bar",
                "friends":[
                    {"id": "$f1"},
                    {"id": "$f2"}
                ]
            }
        """.trimIndent()

        feed(HttpVerb.GET, "/api/users/{id}", 200, payload)
        assertTrue(pool.hasExactKey("userid"))
        assertTrue(pool.hasExactKey("friendid"))

        assertEquals(id, pool.extractValue("userid"))
        assertEquals(id, pool.extractValue("id", "users"))

        val friends = pool.extractAllWithExactKey("friendid")
        assertEquals(2, friends.size)
        assertTrue(friends.contains(f1))
        assertTrue(friends.contains(f2))
    }


    @Test
    fun testPostBase(){

        val id = "123456"
        val payload = """
            {
                "id": $id,
                "name": "foo",
                "surname": "bar"
            }
        """.trimIndent()

        feed(HttpVerb.POST, "/api/users", 201, payload)
        assertEquals(id, pool.extractValue("userid"))
        assertTrue(pool.hasExactKey("userid"))

        //name and surname should NOT be collected, as POST only deals with ids
        assertEquals(1, pool.keySize())
    }

    @Test
    fun testPostWithQualifierNotExact(){

        val id = "123456"
        val payload = """
            {
                "userrID": $id,
                "name": "foo",
                "surname": "bar"
            }
        """.trimIndent()

        feed(HttpVerb.POST, "/api/users", 201, payload)
        assertEquals(id, pool.extractValue("userid"))
        assertTrue(pool.hasExactKey("userrid"))

        //name and surname should NOT be collected, as POST only deals with ids
        assertEquals(1, pool.keySize())
    }
    @Test
    fun testPostNumber(){

        val id = "123456"
        val payload = "$id"

        feed(HttpVerb.POST, "/api/users", 201, payload)
        assertEquals(id, pool.extractValue("userid"))
        assertTrue(pool.hasExactKey("userid"))
    }
}
