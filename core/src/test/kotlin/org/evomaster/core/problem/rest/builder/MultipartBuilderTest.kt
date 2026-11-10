package org.evomaster.core.problem.rest.builder

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MultipartBuilderTest {

    private val schemaText = """
        openapi: 3.0.0
        info:
          title: multipart
          version: "1.0"
        paths:
          /api/documents:
            post:
              requestBody:
                required: true
                content:
                  multipart/form-data:
                    schema:
                      type: object
                      required: [title, file]
                      properties:
                        title:
                          type: string
                        pages:
                          type: integer
                        metadata:
                          type: object
                          properties:
                            author:
                              type: string
                        file:
                          type: string
                          format: binary
                    encoding:
                      file:
                        contentType: image/png
              responses:
                "200":
                  description: ok
          /api/albums:
            post:
              requestBody:
                required: true
                content:
                  multipart/form-data:
                    schema:
                      type: object
                      required: [attachments]
                      properties:
                        attachments:
                          type: array
                          items:
                            type: string
                            format: binary
                        tags:
                          type: array
                          items:
                            type: string
                    encoding:
                      attachments:
                        contentType: image/jpeg
              responses:
                "200":
                  description: ok
        """.trimIndent()

    private fun buildBody(flag: Boolean, path: String = "/api/documents"): BodyParam {
        val swagger = OpenAPIParser().readContents(schemaText, null, null).openAPI
        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(
            RestSchema(SchemaOpenAPI("", swagger, SchemaLocation.MEMORY)),
            actions,
            options = RestActionBuilderV3.Options(
                enableConstraintHandling = false,
                enableMultipartFormDataSupport = flag
            )
        )
        val action = actions["POST:$path"]
        assertTrue(action is RestCallAction, "action not built: ${actions.keys}")
        return (action as RestCallAction).parameters.filterIsInstance<BodyParam>().first()
    }

    @Test
    fun testPartsExtractedFromSchema() {
        val body = buildBody(flag = true)

        assertEquals(setOf("title", "pages", "metadata", "file"), body.parts.keys)

        //only format: binary is a file
        assertTrue(body.parts["file"]!!.isFile)
        assertFalse(body.parts["title"]!!.isFile)
        assertFalse(body.parts["pages"]!!.isFile)
        assertFalse(body.parts["metadata"]!!.isFile)

        //encoding is read only for what the schema declares
        assertEquals("image/png", body.parts["file"]!!.contentType)
        assertNull(body.parts["title"]!!.contentType)
    }

    @Test
    fun testFlagOffLeavesEverythingAsBefore() {
        val body = buildBody(flag = false)

        assertTrue(body.parts.isEmpty())
        assertFalse(body.isMultipartSupported())
        //multipart still unsupported
        assertTrue(body.notSupportedContentTypes.any { it.contains("multipart") })
    }

    @Test
    fun testFlagOnMakesMultipartASupportedType() {
        val body = buildBody(flag = true)
        assertTrue(body.notSupportedContentTypes.isEmpty(),
            "with the metadata available, multipart is supported: ${body.notSupportedContentTypes}")
    }

    @Test
    fun testFullEnvelopeFromARealSchema() {
        val body = buildBody(flag = true)

        val randomness = Randomness().apply { updateSeed(42) }
        body.seeGenes().forEach { it.doInitialize(randomness) }

        //doInitialize randomizes the content type, so select multipart afterwards
        val i = body.contentTypeGene.values.indexOfFirst { it.contains("multipart") }
        assertTrue(i >= 0, "multipart not among ${body.contentTypeGene.values}")
        body.contentTypeGene.index = i

        assertTrue(body.isMultipartSupported())

        val out = body.getValueAsPrintableString(
            mode = GeneUtils.EscapeMode.MULTIPART_FORM_DATA,
            targetFormat = OutputFormat.KOTLIN_JUNIT_5
        )

        val boundary = body.multipartBoundary()
        assertTrue(out.startsWith("--$boundary\r\n"))
        assertTrue(out.endsWith("--$boundary--\r\n"))

        //required parts are always there
        assertTrue(out.contains("name=\"title\""), out)
        assertTrue(out.contains("name=\"file\"; filename=\"file.png\""), out)
        assertTrue(out.contains("Content-Type: image/png"), out)

        //the object part goes as JSON
        if (out.contains("name=\"metadata\"")) {
            assertTrue(out.contains("Content-Type: application/json"), out)
        }

        //same boundary in header and body
        assertEquals("multipart/form-data; boundary=$boundary", body.contentTypeHeader())
    }

    @Test
    fun testBinaryFormatOnArrayItemsMakesThePropertyAFile() {
        //for arrays, `format: binary` is on the items
        val body = buildBody(flag = true, path = "/api/albums")

        assertEquals(setOf("attachments", "tags"), body.parts.keys)

        assertTrue(body.parts["attachments"]!!.isFile)
        assertEquals("image/jpeg", body.parts["attachments"]!!.contentType)

        //an array of plain strings is not a file, and nothing was declared for it
        assertFalse(body.parts["tags"]!!.isFile)
        assertNull(body.parts["tags"]!!.contentType)
    }

    @Test
    fun testEnvelopeFromARealSchemaRepeatsTheNameOfAnArrayProperty() {
        val body = buildBody(flag = true, path = "/api/albums")

        val randomness = Randomness().apply { updateSeed(42) }
        body.seeGenes().forEach { it.doInitialize(randomness) }
        body.contentTypeGene.index = body.contentTypeGene.values.indexOfFirst { it.contains("multipart") }

        val obj = body.primaryGene().getWrappedGene(ObjectGene::class.java)!!
        //activate all optional fields
        obj.fixedFields.filterIsInstance<OptionalGene>().forEach { it.isActive = true }

        val arr = obj.fixedFields.first { it.name == "attachments" }.getLeafGene() as ArrayGene<*>
        //fix size to two
        arr.killAllChildren()
        listOf("AAA", "BBB").forEach { v ->
            val e = StringGene("attachments")
            e.doInitialize(randomness)
            e.value = v
            arr.addElement(e)
        }

        val out = body.getValueAsPrintableString(
            mode = GeneUtils.EscapeMode.MULTIPART_FORM_DATA,
            targetFormat = OutputFormat.KOTLIN_JUNIT_5
        )

        assertEquals(2, Regex("name=\"attachments\"").findAll(out).count(), out)
        assertTrue(out.contains("filename=\"attachments_0.jpg\""), out)
        assertTrue(out.contains("filename=\"attachments_1.jpg\""), out)
        assertEquals(2, Regex("Content-Type: image/jpeg").findAll(out).count(), out)
    }
}
