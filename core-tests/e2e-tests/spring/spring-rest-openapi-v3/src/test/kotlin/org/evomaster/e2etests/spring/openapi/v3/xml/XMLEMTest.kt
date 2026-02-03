package org.evomaster.e2etests.spring.openapi.v3.xml

import com.foo.rest.examples.spring.openapi.v3.xml.XMLController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test



/**
 * White-box E2E test for XML handling with attributes.
 *
 * This test verifies that EvoMaster can properly:
 * 1. Handle REST APIs that accept both JSON and XML payloads on the same endpoint
 * 2. Parse XML attributes (@XmlAttribute) correctly, not just elements (@XmlElement)
 * 3. Generate test cases that cover different branches based on attribute values
 *
 * Key scenarios tested:
 * - /product: Dual JSON/XML endpoint with @XmlAttribute for 'sku' field
 * - /order: XML-only endpoint with nested objects containing @XmlAttribute
 * - /author: JSON-only endpoint (for comparison)
 * - /create-product: Endpoint that returns XML with attributes
 */
class XMLEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(XMLController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "XMLEM",
            "org.foo.XMLEM",
            2000,
            true,
            { args: MutableList<String> ->

                val solution = initAndRun(args)
                assertTrue(solution.individuals.size >= 1)

                /* ========= string / person ========= */
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/receive-string-respond-xml", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/receive-string-respond-xml", null)

                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/receive-xml-respond-string", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/receive-xml-respond-string", null)

                /* ========= nesting ========= */
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/employee", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/employee", null)

                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/company", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/company", null)

                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/department", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/department", null)

                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/organization", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/organization", null)

                /* ========= attributes ========= */
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/project", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/project", null)

                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/projects", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/projects", null)

                /* ========= person with attributes ========= */
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/person-with-attr", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/xml/person-with-attr", null)

            },
            3,
        )

    }

}