package org.evomaster.core.output

import org.evomaster.core.TestUtils
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import org.evomaster.core.output.EvaluatedIndividualBuilder.Companion.buildResourceEvaluatedIndividual
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

class PythonTestCaseWriterTest : WriterTestBase(){


    @Test
    fun testEmptyTest() {


        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(emptyList<SqlAction>().toMutableList())

        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())

        val lines = writer.convertToCompilableTestCode(test, baseUrlOfSut)

        val expectedLines = Lines(format).apply {
            add("def test(self):")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }

    private fun buildEvaluatedIndividual(dbInitialization: MutableList<SqlAction>): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {
        val format = OutputFormat.PYTHON_UNITTEST

        val baseUrlOfSut = "baseUrlOfSut"

        val sampleType = SampleType.RANDOM

        val restActions = emptyList<RestCallAction>().toMutableList()

        val individual = RestIndividual(restActions, sampleType, dbInitialization)
        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)

        val results = dbInitialization.map { SqlActionResult(it.getLocalId()).also { it.setInsertExecutionResult(true) } }

        val ei = EvaluatedIndividual(fitnessVal, individual, results)
        return Triple(format, baseUrlOfSut, ei)
    }

    @Test
    fun testSimpleRequestWithAcceptHeader(){
        val format = OutputFormat.PYTHON_UNITTEST

        val baseUrlOfSut = "baseUrlOfSut"
        val sampleType = SampleType.RANDOM
        val action = RestCallAction("1", HttpVerb.GET, RestPath("/"), mutableListOf())
        val restActions = listOf(action).toMutableList()
        val individual = RestIndividual(restActions, sampleType)
        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)
        val result = RestCallResult(action.getLocalId())
        result.setTimedout(timedout = true)
        val results = listOf(result)
        val ei = EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
        val config = getConfig(format)
        //config.expectationsActive = true

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())

        val lines = writer.convertToCompilableTestCode( test, baseUrlOfSut)

        val expectedLines = Lines(format).apply {
            add("def test(self):")
            indent()
            add("")
            add("try:")
            indent()
            add("headers = {}")
            add("headers['Accept'] = \"*/*\"")
            add("requests \\")
            indent()
            indent()
            add(".get(self.baseUrlOfSut + \"/\",")
            indent()
            add("headers=headers)")
            deindent()
            deindent()
            deindent()
            deindent()
            add("except AssertionError as e:")
            indent()
            add("raise e")
            deindent()
            add("except Exception as e:")
            indent()
            add("pass")
            deindent()
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }

    @Test
    fun testTestWithObjectAssertion(){
        val fooAction = RestCallAction("1", HttpVerb.GET, RestPath("/foo"), mutableListOf())

        val (format, baseUrlOfSut, ei) = buildResourceEvaluatedIndividual(
            dbInitialization = mutableListOf(),
            groups = mutableListOf(
                (mutableListOf<SqlAction>() to mutableListOf(fooAction))
            ),
            format = OutputFormat.PYTHON_UNITTEST
        )

        val fooResult = ei.seeResult(fooAction.getLocalId()) as RestCallResult
        fooResult.setTimedout(false)
        fooResult.setStatusCode(200)
        fooResult.setBody("""
           [
                {},
                {
                    "id":"foo",
                    "properties":[
                        {},
                        {
                          "name":"mapProperty1",
                          "type":"string",
                          "value":"one"
                        },
                        {
                          "name":"mapProperty2",
                          "type":"string",
                          "value":"two"
                        }],
                    "empty":{}
                }
           ]
        """.trimIndent())
        fooResult.setBodyType(MediaType.APPLICATION_JSON_TYPE)


        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())
        val lines = writer.convertToCompilableTestCode( test, baseUrlOfSut)

        val expectedLines = """
            def test(self):
                
                headers = {}
                headers['Accept'] = "*/*"
                res_0 = requests \
                        .get(self.baseUrlOfSut + "/foo",
                            headers=headers)
                
                assert res_0.status_code == 200
                assert "application/json" in res_0.headers["content-type"]
                assert len(res_0.json()) == 2
                assert len(res_0.json()[0]) == 0
                assert len(res_0.json()[1]["properties"]) == 3
                assert len(res_0.json()[1]["properties"][0]) == 0
                assert res_0.json()[1]["properties"][1]["name"] == "mapProperty1"
                assert res_0.json()[1]["properties"][1]["type"] == "string"
                assert res_0.json()[1]["properties"][1]["value"] == "one"
                assert res_0.json()[1]["properties"][2]["name"] == "mapProperty2"
                assert res_0.json()[1]["properties"][2]["type"] == "string"
                assert res_0.json()[1]["properties"][2]["value"] == "two"
                assert len(res_0.json()[1]["empty"]) == 0

""".trimIndent()

        assertEquals(expectedLines, lines.toString())
    }


    @Test
    fun testTestWithObjectLengthAssertion(){
        val fooAction = RestCallAction("1", HttpVerb.GET, RestPath("/foo"), mutableListOf())

        val (format, baseUrlOfSut, ei) = buildResourceEvaluatedIndividual(
            dbInitialization = mutableListOf(),
            groups = mutableListOf(
                (mutableListOf<SqlAction>() to mutableListOf(fooAction))
            ),
            format = OutputFormat.PYTHON_UNITTEST
        )

        val fooResult = ei.seeResult(fooAction.getLocalId()) as RestCallResult
        fooResult.setTimedout(false)
        fooResult.setStatusCode(200)
        fooResult.setBody("""
           {
                "p1":{},
                "p2":{
                    "id":"foo",
                    "properties":[
                        {},
                        {
                          "name":"mapProperty1",
                          "type":"string",
                          "value":"one"
                        },
                        {
                          "name":"mapProperty2",
                          "type":"string",
                          "value":"two"
                        }],
                    "empty":{}
                }
           }
        """.trimIndent())
        fooResult.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())
        val lines = writer.convertToCompilableTestCode( test, baseUrlOfSut)

        val expectedLines = """
            def test(self):
                
                headers = {}
                headers['Accept'] = "*/*"
                res_0 = requests \
                        .get(self.baseUrlOfSut + "/foo",
                            headers=headers)
                
                assert res_0.status_code == 200
                assert "application/json" in res_0.headers["content-type"]
                assert len(res_0.json()["p1"]) == 0
                assert len(res_0.json()["p2"]["properties"]) == 3
                assert len(res_0.json()["p2"]["properties"][0]) == 0
                assert res_0.json()["p2"]["properties"][1]["name"] == "mapProperty1"
                assert res_0.json()["p2"]["properties"][1]["type"] == "string"
                assert res_0.json()["p2"]["properties"][1]["value"] == "one"
                assert res_0.json()["p2"]["properties"][2]["name"] == "mapProperty2"
                assert res_0.json()["p2"]["properties"][2]["type"] == "string"
                assert res_0.json()["p2"]["properties"][2]["value"] == "two"
                assert len(res_0.json()["p2"]["empty"]) == 0

""".trimIndent()
        assertEquals(expectedLines, lines.toString())
    }


    @Test
    fun testApplyAssertionEscapes(){
        val fooAction = RestCallAction("1", HttpVerb.GET, RestPath("/foo"), mutableListOf())

        val (format, baseUrlOfSut, ei) = buildResourceEvaluatedIndividual(
            dbInitialization = mutableListOf(),
            groups = mutableListOf(
                (mutableListOf<SqlAction>() to mutableListOf(fooAction))
            ),
            format = OutputFormat.PYTHON_UNITTEST
        )

        val fooResult = ei.seeResult(fooAction.getLocalId()) as RestCallResult
        val email = "foo@foo.foo"
        fooResult.setTimedout(false)
        fooResult.setStatusCode(200)
        fooResult.setBody("""
           {
                "email":"$email"
           }
        """.trimIndent())
        fooResult.setBodyType(MediaType.APPLICATION_JSON_TYPE)



        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())
        val lines = writer.convertToCompilableTestCode( test, baseUrlOfSut)

        val expectedLines = """
            def test(self):
                
                headers = {}
                headers['Accept'] = "*/*"
                res_0 = requests \
                        .get(self.baseUrlOfSut + "/foo",
                            headers=headers)
                
                assert res_0.status_code == 200
                assert "application/json" in res_0.headers["content-type"]
                assert res_0.json()["email"] == "foo@foo.foo"

""".trimIndent()
        assertEquals(expectedLines, lines.toString())
    }

}
