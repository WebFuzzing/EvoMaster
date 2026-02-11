package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class BBXMLTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBXMLController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "BBXmlEM",
            "org.foo.BBXmlEM",
            null,  // terminations
            1000,   // iterations
            false, // createTests - skip compilation phase
            { args: MutableList<String> ->

                addBlackBoxOptions(args, OutputFormat.JAVA_JUNIT_5)
                args.add("--enableBasicAssertions")
                args.add("true")

                val solution = initAndRun(args)
                assertTrue(solution.individuals.size >= 1)

                /* ========= basic XML endpoints ========= */
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-string-respond-xml", null)
                // 400 for receive-string-respond-xml requires blank string - hard to generate

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

                /* ========= XML with attributes (main test objective) ========= */

                // person-with-attr: simple object with @XmlAttribute
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/person-with-attr", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/person-with-attr", null)

                // project: object with @XmlAttribute and list of PersonWithAttr
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/project", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/project", null)

                // projects: list of Project objects
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", null)
            },
            3  // timeoutMinutes
        )
    }
}