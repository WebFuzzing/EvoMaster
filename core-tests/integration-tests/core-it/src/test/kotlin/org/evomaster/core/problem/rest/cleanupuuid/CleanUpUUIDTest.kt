package org.evomaster.core.problem.rest.cleanupuuid

import bar.examples.it.spring.cleanupuuid.CleanUpUUIDApplication
import bar.examples.it.spring.cleanupuuid.CleanUpUUIDController
import bar.examples.it.spring.cleanupuuid.CleanUpUUIDDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CleanUpUUIDTest: IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CleanUpUUIDController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        CleanUpUUIDApplication.reset()
        recreateInjectorForBlack(listOf("--blackBoxCleanUp","true"))
    }

    private fun getReturnedSize(result: RestCallResult) : Int{
        val bodyString = result.getBody()!!
        val mapper = ObjectMapper()
        val dto = mapper.readValue(bodyString, CleanUpUUIDDto::class.java)
        return dto.size!!
    }

    @Test
    fun testPutDelete() {

        assertEquals(0, CleanUpUUIDApplication.numberExistingData())

        val pirTest = getPirToRest()

        val id = UUID.randomUUID().toString()

        val put = pirTest.fromVerbPath("put", "/api/resources/$id")!!

        val x = createIndividual(listOf(put), SampleType.RANDOM)
        val resDel = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(201, resDel.getStatusCode())
        assertEquals(1, getReturnedSize(resDel))

        assertEquals(1, x.evaluatedMainActions().size)
        assertEquals(1, x.individual.seeMainExecutableActions().size)
        assertEquals(1, x.individual.seeCleanUpActions().size)

        //delete should had been automatically added
        assertEquals(0, CleanUpUUIDApplication.numberExistingData())
    }

    @Test
    fun testMultipleDistinctPutDelete() {

        assertEquals(0, CleanUpUUIDApplication.numberExistingData())

        val pirTest = getPirToRest()

        val id0 = UUID.randomUUID().toString()
        val put0 = pirTest.fromVerbPath("put", "/api/resources/$id0")!!
        val id1 = UUID.randomUUID().toString()
        val put1 = pirTest.fromVerbPath("put", "/api/resources/$id1")!!
        val id2 = UUID.randomUUID().toString()
        val put2 = pirTest.fromVerbPath("put", "/api/resources/$id2")!!

        val x = createIndividual(listOf(put0,put1,put2), SampleType.RANDOM)

        val resDel0 = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(201, resDel0.getStatusCode())
        assertEquals(1, getReturnedSize(resDel0))
        val resDel1 = x.evaluatedMainActions()[1].result as RestCallResult
        assertEquals(201, resDel1.getStatusCode())
        assertEquals(2, getReturnedSize(resDel1))
        val resDel2 = x.evaluatedMainActions()[2].result as RestCallResult
        assertEquals(201, resDel2.getStatusCode())
        assertEquals(3, getReturnedSize(resDel2))

        assertEquals(3, x.evaluatedMainActions().size)
        assertEquals(3, x.individual.seeMainExecutableActions().size)
        assertEquals(3, x.individual.seeCleanUpActions().size)

        //delete should had been automatically added
        assertEquals(0, CleanUpUUIDApplication.numberExistingData())
    }

    @Test
    fun testMultipleRepeatedPutDelete() {

        assertEquals(0, CleanUpUUIDApplication.numberExistingData())

        val pirTest = getPirToRest()

        val id0 = UUID.randomUUID().toString()
        val put0 = pirTest.fromVerbPath("put", "/api/resources/$id0")!!
        val id1 = UUID.randomUUID().toString()
        val put1 = pirTest.fromVerbPath("put", "/api/resources/$id1")!!
        //this is using same id as first call, so replace, does not create new resource
        val put2 = pirTest.fromVerbPath("put", "/api/resources/$id0")!!

        val x = createIndividual(listOf(put0,put1,put2), SampleType.RANDOM)

        val resDel0 = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(201, resDel0.getStatusCode())
        assertEquals(1, getReturnedSize(resDel0))
        val resDel1 = x.evaluatedMainActions()[1].result as RestCallResult
        assertEquals(201, resDel1.getStatusCode())
        assertEquals(2, getReturnedSize(resDel1))
        val resDel2 = x.evaluatedMainActions()[2].result as RestCallResult
        assertEquals(200, resDel2.getStatusCode())
        assertEquals(2, getReturnedSize(resDel2))

        assertEquals(3, x.evaluatedMainActions().size)
        assertEquals(3, x.individual.seeMainExecutableActions().size)
        assertEquals(2, x.individual.seeCleanUpActions().size)

        //delete should had been automatically added
        assertEquals(0, CleanUpUUIDApplication.numberExistingData())
    }


    @Test
    fun testPostDelete() {

        assertEquals(0, CleanUpUUIDApplication.numberExistingData())

        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/resources")!!

        val x = createIndividual(listOf(post), SampleType.RANDOM)
        val resDel = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(201, resDel.getStatusCode())
        assertEquals(1, getReturnedSize(resDel))

        assertEquals(1, x.evaluatedMainActions().size)
        assertEquals(1, x.individual.seeMainExecutableActions().size)
        assertEquals(1, x.individual.seeCleanUpActions().size)

        //delete should had been automatically added
        assertEquals(0, CleanUpUUIDApplication.numberExistingData())
    }

    //TODO multi distinct POST
    //TODO mixed POST PUT
    //TODO mixed multi POST PUT repeated
    //TODO merge (various combinations)
    //TODO mutation remove POST/PUT (should remove delete)
}