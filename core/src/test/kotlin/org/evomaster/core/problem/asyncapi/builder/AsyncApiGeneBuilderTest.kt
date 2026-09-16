package org.evomaster.core.problem.asyncapi.builder

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiGeneBuilderTest {

    private val options = AsyncApiGeneBuilder.options(EMConfig())

    /**
     * RestActionBuilderV3 keeps its built genes in a static cache, so a test that did not clear
     * it could be handed one built by another.
     */
    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun payloadOf(resourcePath: String, messageId: String): Gene {
        val schema = AsyncApiAccess.getAsyncApiFromResource(resourcePath)
        return AsyncApiGeneBuilder.buildPayloadGene(schema, schema.messages.getValue(messageId), options)!!
    }

    private fun payloadOf(schema: AsyncApiDocument, messageId: String): Gene =
        AsyncApiGeneBuilder.buildPayloadGene(schema, schema.messages.getValue(messageId), options)!!

    /**
     * The gene a field ended up with, unwrapping the optional that a non-required field gets.
     */
    private fun field(gene: Gene, name: String): Gene {
        val obj = gene as ObjectGene
        val found = obj.fields.first { it.name == name }
        return if (found is OptionalGene) found.gene else found
    }

    // ------------------------------------------------------------------ the shape of a payload

    @Test
    fun testObjectWithRequiredAndOptionalFields() {

        val gene = payloadOf("/asyncapi/artificial/messages.yaml", "signupReply")

        assertTrue(gene is ObjectGene)
        //both fields are required here, so neither is wrapped in an optional
        assertTrue((gene as ObjectGene).fields.all { it !is OptionalGene })
        assertTrue(field(gene, "request_id") is StringGene)
        assertTrue(field(gene, "userId") is StringGene)
    }

    @Test
    fun testPayloadDeclaredAsAReferenceIsResolved() {

        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/messages.yaml")
        val gene = payloadOf(schema, "signupRequest")

        //the payload is nothing but a $ref, so the gene is named after the schema it points at
        assertEquals("SignupRequest", gene.name)

        val email = field(gene, "email")
        assertTrue(email is StringGene, "expected a string, got ${email.javaClass.simpleName}")

        //and the schema that one reaches in turn is resolved too
        assertTrue(field(gene, "address") is ObjectGene)
    }

    @Test
    fun testNumericBoundsAreKept() {

        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/sut/ncs-kafka.yaml")
        val gene = payloadOf(schema, "bessjRequest")

        /*
            This is the whole point of the search for this service: the reply is an error below
            n = 3, so the bound has to survive into the gene for the boundary to be findable.
         */
        val n = field(gene, "n") as IntegerGene
        assertEquals(3, n.min)
        assertEquals(1000, n.max)
    }

    @Test
    fun testHeadersAreBuiltSeparatelyFromThePayload() {

        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/messages.yaml")

        //a message declaring no headers gets none, rather than an empty object
        assertNull(
            AsyncApiGeneBuilder.buildHeadersGene(schema, schema.messages.getValue("heartbeat"), options)
        )

        /*
            signupRequest declares one header, and it is the one the correlation id is stamped
            into. That value is written fresh at each execution, so a gene holding it would only
            be overwritten -- and with nothing else left, there are no header genes at all.
         */
        assertNull(
            AsyncApiGeneBuilder.buildHeadersGene(schema, schema.messages.getValue("signupRequest"), options)
        )
    }

    @Test
    fun testMessageWithoutAPayload() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: A message with nothing in it
              version: 1.0.0
            components:
              messages:
                empty:
                  name: Empty
            """.trimIndent()
        )

        assertNull(
            AsyncApiGeneBuilder.buildPayloadGene(schema, schema.messages.getValue("empty"), options)
        )
    }

    // ------------------------------------------------------------------ const

    @Test
    fun testConstBecomesTheSingleValueItPinsTheFieldTo() {

        val gene = payloadOf("/asyncapi/artificial/websocket-reply.yaml", "listLegs")

        /*
            'request' is declared `const: list_legs`, which is how this document says which
            message a payload is. The gene builder does not read `const`, so without a rewrite
            the field would be a free string and the service would not recognise the message.
         */
        val request = field(gene, "request")
        assertTrue(request is EnumGene<*>, "expected an enum, got ${request.javaClass.simpleName}")
        assertEquals(listOf("list_legs"), (request as EnumGene<*>).values)
    }

    @Test
    fun testConstIsPinnedExactlyRatherThanFuzzed() {

        val gene = payloadOf("/asyncapi/artificial/websocket-reply.yaml", "listLegs")

        /*
            A declared enum normally gains a bogus member on purpose, to probe how a service
            handles a value it never declared. A discriminator must not: a message carrying one
            the service does not know is dropped, which would waste half the executions and
            fire the no-reply fault target for a fault of our own making.
         */
        assertFalse((field(gene, "request") as EnumGene<*>).values.any { it.toString() == "EVOMASTER" })
    }

    @Test
    fun testConstWithAnExplicitTypeAndConstOfOtherKinds() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: Constants of several kinds
              version: 1.0.0
            components:
              messages:
                m:
                  payload:
                    type: object
                    properties:
                      typed:
                        type: string
                        const: fixed
                      count:
                        const: 7
                      ratio:
                        const: 1.5
                      flag:
                        const: true
            """.trimIndent()
        )

        val gene = payloadOf(schema, "m")

        //a const alongside an explicit type is just as invisible to the gene builder as without
        assertEquals(listOf("fixed"), (field(gene, "typed") as EnumGene<*>).values)

        /*
            A number is pinned with equal bounds rather than a one-value enum. The builder adds
            a bogus member to a numeric enum unconditionally, so an enum of one value would
            always come back as two, and the discriminator would be wrong half the time.
         */
        val count = field(gene, "count") as IntegerGene
        assertEquals(7, count.min)
        assertEquals(7, count.max)

        val ratio = field(gene, "ratio") as DoubleGene
        assertEquals(1.5, ratio.min)
        assertEquals(1.5, ratio.max)

        /*
            A boolean is the exception, and it is documented rather than fixed: there is no enum
            handling for booleans and no bounds to set. It matters little in practice, since a
            two-valued discriminator is guessed half the time anyway.
         */
        assertTrue(field(gene, "flag") is BooleanGene)
    }

    @Test
    fun testRewritingConstDoesNotTouchTheParsedDocument() {

        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/websocket-reply.yaml")
        val payload = schema.messages.getValue("listLegs").payload!!

        AsyncApiGeneBuilder.buildPayloadGene(schema, schema.messages.getValue("listLegs"), options)

        //the model is shared, so building genes from it must not rewrite it underneath
        assertTrue(payload.get("properties").get("request").has("const"))
        assertFalse(payload.get("properties").get("request").has("enum"))
    }

    // ------------------------------------------------------------------ the harder schemas

    @Test
    fun testDeclaredEnumsKeepTheirOwnValues() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: A genuine enum
              version: 1.0.0
            components:
              messages:
                m:
                  payload:
                    type: object
                    properties:
                      kind:
                        type: string
                        enum: [ok, error]
            """.trimIndent()
        )

        val kind = field(payloadOf(schema, "m"), "kind") as EnumGene<*>

        assertEquals(setOf("ok", "error"), kind.values.map { it.toString() }.toSet())
    }

    @Test
    fun testOneOfBecomesAChoice() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: A reply that is one of two shapes
              version: 1.0.0
            components:
              messages:
                m:
                  payload:
                    oneOf:
                      - type: object
                        properties:
                          result:
                            type: integer
                      - type: object
                        properties:
                          error:
                            type: string
            """.trimIndent()
        )

        val gene = payloadOf(schema, "m")

        assertTrue(gene is ChoiceGene<*>, "expected a choice, got ${gene.javaClass.simpleName}")
        assertEquals(2, (gene as ChoiceGene<*>).getViewOfChildren().size)
    }

    @Test
    fun testASchemaThatRefersToItselfTerminates() {

        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/reference-cycles.yaml")

        //a tree of nodes is legitimate, and must neither recurse for ever nor be dropped
        val gene = payloadOf(schema, "request")

        assertTrue(gene is ObjectGene)
        assertTrue(field(gene, "children") is ArrayGene<*>)
    }

    // ------------------------------------------------------------------ pointers into a schema

    @Test
    fun testPayloadPointingAtOnePropertyOfASchema() {

        /*
            The parser accepts a pointer deeper than the schema it names, checking only that the
            schema is there. Built naively that becomes the whole schema -- a different message
            entirely -- because the OpenAPI machinery underneath follows a reference to a whole
            schema and nothing else.
         */
        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: A pointer into a schema
              version: 1.0.0
            components:
              messages:
                m:
                  payload:
                    ${'$'}ref: '#/components/schemas/Order/properties/item'
              schemas:
                Order:
                  type: object
                  properties:
                    item:
                      type: string
                      minLength: 7
                    quantity:
                      type: integer
            """.trimIndent()
        )

        val gene = payloadOf(schema, "m")

        assertTrue(gene is StringGene, "expected the property, got ${gene.javaClass.simpleName}")
        assertEquals(7, (gene as StringGene).minLength)
    }

    @Test
    fun testPayloadPointingAtAPropertyThatIsNotThere() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: A pointer that leads nowhere
              version: 1.0.0
            components:
              messages:
                m:
                  payload:
                    ${'$'}ref: '#/components/schemas/Order/properties/absent'
              schemas:
                Order:
                  type: object
                  properties:
                    item:
                      type: string
            """.trimIndent()
        )

        //better to say so than to hand back a message with no fields and no explanation
        val e = assertThrows(IllegalArgumentException::class.java) {
            AsyncApiGeneBuilder.buildPayloadGene(schema, schema.messages.getValue("m"), options)
        }

        assertTrue(e.message!!.contains("absent"), e.message)
    }

    @Test
    fun testPayloadReferringToAWholeSchemaStillUsesItsName() {

        //the shortcut that names the gene after the document's own schema must still apply
        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/messages.yaml")
        val gene = payloadOf(schema, "signupRequest")

        assertEquals("SignupRequest", gene.name)
    }

    // ------------------------------------------------------------------ where const is not a keyword

    @Test
    fun testConstIsRewrittenOnlyWhereItIsAKeyword() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: The word const in other positions
              version: 1.0.0
            components:
              messages:
                m:
                  payload:
                    type: object
                    default:
                      const: this is a value, not a keyword
                    properties:
                      const:
                        type: string
                        const: still a keyword one level down
            """.trimIndent()
        )

        val gene = payloadOf(schema, "m")

        /*
            A field the document happens to call "const" is still a field, and the const *it*
            declares is still a keyword. Skipping the word wherever it appears would lose this
            one, which is why the maps of name-to-schema are walked rather than read as schemas.
         */
        assertEquals(
            listOf("still a keyword one level down"),
            (field(gene, "const") as EnumGene<*>).values
        )

        //and the parsed document is never the thing rewritten: it belongs to the parser
        val payload = schema.messages.getValue("m").payload!!
        assertTrue(payload.get("default").has("const"))
        assertFalse(payload.get("properties").get("const").has("enum"))
    }

    @Test
    fun testEverySchemaAPayloadCanReachIsAvailable() {

        /*
            The parser drops any message whose schema reaches a reference it cannot follow, so
            everything that survives must be buildable. Assert that over a real document rather
            than trusting it.
         */
        val schema = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/sut/ncs-kafka.yaml")

        schema.messages.values.forEach { message ->
            assertNotNull(
                AsyncApiGeneBuilder.buildPayloadGene(schema, message, options),
                "no gene could be built for message '${message.id}'"
            )
        }
    }
}
