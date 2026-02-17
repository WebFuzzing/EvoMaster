package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
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
    fun testRunEM(outputFormat: OutputFormat) {

        assumeTrue(outputFormat == OutputFormat.JAVA_JUNIT_5 || outputFormat == OutputFormat.JAVA_JUNIT_4)

        executeAndEvaluateBBTest(
            outputFormat,
            "BBXmlEM",
            1000,
            3,
            listOf("STRING_TO_XML", "XML_TO_STRING", "EMPLOYEE", "COMPANY",
                "DEPARTMENT", "ORGANIZATION", "PERSON_ATTR", "PROJECT", "PROJECTS")
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.JAVA_JUNIT_5)

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
}