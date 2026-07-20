package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.CoveredTargets
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBXMLTest : SpringTestBase() {

    companion object {

        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBXMLController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "BBXmlEM",
            1000,
            3,
            listOf("STRING_TO_XML", "XML_TO_STRING", "EMPLOYEE", "COMPANY",
                "DEPARTMENT", "ORGANIZATION", "PERSON_ATTR", "PROJECT", "PROJECTS")
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            /* ========= basic XML endpoints ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-string-respond-xml", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-xml-respond-string", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/receive-xml-respond-string", null)

            /* ========= nested XML objects ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/employee", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/employee", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/company", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/company", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/department", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/department", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/organization", null)

            /* ========= XML with attributes ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/person-with-attr", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/person-with-attr", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/project", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/project", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", null)
        }
    }

    @Test
    fun testBlackBoxWithoutXmlBodySupport() {
        // These targets require a well-formed XML body to reach a 200 response.
        // With disableXMLSupport=true, EvoMaster falls back to generic field
        // naming (schema ref name or 'body') instead of the actual JAXB element names,
        // so Spring's XML deserializer receives structurally wrong documents and returns
        // 400 for most requests → the 200-branch targets stay uncovered.
        val xmlBodyTargets = listOf(
            "XML_TO_STRING",
            "EMPLOYEE",
            "COMPANY",
            "DEPARTMENT",
            "ORGANIZATION",
            "PERSON_ATTR",
            "PROJECT",
            "PROJECTS"
        )

        runBlackBoxEM(OutputFormat.KOTLIN_JUNIT_5, "BBXmlEM_NoSupport", 1000, 3, false) { args ->
            setOption(args, "disableXMLSupport", "true")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)
        }

        val coveredWithFlagOff = xmlBodyTargets.count { CoveredTargets.isCovered(it) }
        println("=== Flag OFF: $coveredWithFlagOff/${xmlBodyTargets.size} XML body targets covered ===")
        xmlBodyTargets.forEach { target ->
            println("  [${if (CoveredTargets.isCovered(target)) "X" else " "}] $target")
        }

        assertFalse(
            CoveredTargets.areCovered(xmlBodyTargets),
            "With disableXMLSupport=true, EvoMaster should NOT cover all XML body targets"
        )
    }
}