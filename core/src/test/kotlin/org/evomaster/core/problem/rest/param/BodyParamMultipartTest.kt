package org.evomaster.core.problem.rest.param

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BodyParamMultipartTest {

    private val CRLF = "\r\n"

    private fun body(
        parts: Map<String, PartMeta>,
        fields: List<org.evomaster.core.search.gene.Gene>,
        contentType: String = "multipart/form-data"
    ): BodyParam {
        val typeGene = EnumGene("contentType", listOf(contentType))
        typeGene.index = 0
        val b = BodyParam(ObjectGene("body", fields), typeGene, null, parts)
        //urlencoded is added next to multipart and EnumGene sorts values, so select it explicitly
        val i = b.contentTypeGene.values.indexOf(contentType)
        assertTrue(i >= 0, "content type $contentType not among ${b.contentTypeGene.values}")
        b.contentTypeGene.index = i
        return b
    }

    private fun str(name: String, value: String) = StringGene(name).also { it.value = value }

    private fun simpleBody(): BodyParam {
        val title = str("title", "Quarterly report")
        val file = str("file", "aK3zR0fq")
        return body(
            mapOf(
                "title" to PartMeta(isFile = false),
                "file" to PartMeta(isFile = true, contentType = "image/png")
            ),
            listOf(title, file)
        )
    }

    private fun render(b: BodyParam) = b.getValueAsPrintableString(
        mode = GeneUtils.EscapeMode.MULTIPART_FORM_DATA,
        targetFormat = OutputFormat.KOTLIN_JUNIT_5
    )

    @Test
    fun testEnvelopeStructure() {
        val b = simpleBody()
        val out = render(b)

        val boundary = b.multipartBoundary()
        assertEquals(BodyParam.DEFAULT_BOUNDARY, boundary)

        assertTrue(out.startsWith("--$boundary$CRLF"), "must open with the boundary delimiter")
        assertTrue(out.endsWith("--$boundary--$CRLF"), "must close with the terminating delimiter")

        // one delimiter per part, plus the closing one
        assertEquals(3, out.split("--$boundary").size - 1)
    }

    @Test
    fun testTextPartHasNoFilenameAndIsNotQuoted() {
        val out = render(simpleBody())

        assertTrue(out.contains("Content-Disposition: form-data; name=\"title\"$CRLF"))
        assertTrue(out.contains("Content-Type: text/plain"))
        // the raw value, not a quoted JSON string
        assertTrue(out.contains("${CRLF}Quarterly report$CRLF"))
        assertFalse(out.contains("\"Quarterly report\""))
    }

    @Test
    fun testFilePartCarriesFilenameWithDerivedExtension() {
        val out = render(simpleBody())

        assertTrue(
            out.contains("Content-Disposition: form-data; name=\"file\"; filename=\"file.png\""),
            "a file part needs a filename, and its extension comes from the part content type"
        )
        assertTrue(out.contains("Content-Type: image/png"))
    }

    @Test
    fun testExtensionFallsBackToBinWhenEncodingIsNotDeclared() {
        val b = body(
            mapOf("file" to PartMeta(isFile = true)),
            listOf(str("file", "xyz"))
        )
        val out = render(b)

        assertTrue(out.contains("filename=\"file.bin\""))
        assertTrue(out.contains("Content-Type: application/octet-stream"))
    }

    @Test
    fun testDeclaredEncodingWinsOverTheDefault() {
        val b = body(
            mapOf("doc" to PartMeta(isFile = true, contentType = "application/pdf")),
            listOf(str("doc", "%PDF"))
        )
        val out = render(b)

        assertTrue(out.contains("filename=\"doc.pdf\""))
        assertTrue(out.contains("Content-Type: application/pdf"))
    }

    @Test
    fun testObjectPartIsSerializedAsJson() {
        val meta = ObjectGene("metadata", listOf(str("author", "Jane")))
        val b = body(mapOf("metadata" to PartMeta(isFile = false)), listOf(meta))
        val out = render(b)

        assertTrue(out.contains("Content-Type: application/json"))
        assertTrue(out.contains("\"author\""), "the object part must be printed as JSON: $out")
    }

    @Test
    fun testInactiveOptionalPartIsOmitted() {
        val pages = OptionalGene("pages", IntegerGene("pages", 12))
        pages.isActive = false

        val b = body(
            mapOf("title" to PartMeta(isFile = false), "pages" to PartMeta(isFile = false)),
            listOf(str("title", "t"), pages)
        )
        val out = render(b)

        assertTrue(out.contains("name=\"title\""))
        assertFalse(out.contains("name=\"pages\""), "an inactive OptionalGene must not produce a part")
    }

    @Test
    fun testBoundaryGetsSuffixOnCollision() {
        val b = body(
            mapOf("title" to PartMeta(isFile = false)),
            listOf(str("title", "junk ${BodyParam.DEFAULT_BOUNDARY} junk"))
        )

        val boundary = b.multipartBoundary()
        assertNotEquals(BodyParam.DEFAULT_BOUNDARY, boundary)
        assertTrue(boundary.startsWith(BodyParam.DEFAULT_BOUNDARY))

        // same boundary in header and body
        val out = render(b)
        assertTrue(out.startsWith("--$boundary$CRLF"))
        assertTrue(b.contentTypeHeader().endsWith("boundary=$boundary"))
    }

    @Test
    fun testContentTypeHeaderCarriesTheBoundaryButContentTypeDoesNot() {
        val b = simpleBody()

        assertEquals("multipart/form-data", b.contentType())
        assertEquals(
            "multipart/form-data; boundary=${BodyParam.DEFAULT_BOUNDARY}",
            b.contentTypeHeader()
        )
        //without parameters
        assertTrue(b.isMultipartForm())
        assertFalse(b.isJson())
        assertFalse(b.isForm())
    }

    @Test
    fun testPartsSurviveCopy() {
        val b = simpleBody()
        val c = b.copy() as BodyParam

        assertEquals(b.parts, c.parts)
        assertEquals(render(b), render(c))
        assertEquals(b.contentTypeHeader(), c.contentTypeHeader())
    }

    @Test
    fun testWithoutPartsTheBehaviourIsUnchanged() {
        //as built when the flag is off
        val b = body(mapOf(), listOf(str("title", "t")))

        assertFalse(b.isMultipartSupported())
        assertTrue(b.isMultipartForm())
        //multipart still unsupported
        assertTrue(b.notSupportedContentTypes.contains("multipart/form-data"))
        //and the header carries no boundary
        assertEquals("multipart/form-data", b.contentTypeHeader())
    }

    @Test
    fun testNonMultipartIsNotAffected() {
        val b = body(mapOf(), listOf(str("name", "x")), contentType = "application/json")

        assertFalse(b.isMultipartSupported())
        assertEquals("application/json", b.contentTypeHeader())
        assertTrue(b.notSupportedContentTypes.isEmpty())
    }

    @Test
    fun testExtensionMapping() {
        assertEquals(".png", BodyParam.extensionForContentType("image/png"))
        assertEquals(".jpg", BodyParam.extensionForContentType("image/jpeg"))
        assertEquals(".pdf", BodyParam.extensionForContentType("application/pdf"))
        assertEquals(".csv", BodyParam.extensionForContentType("text/csv"))
        assertEquals(".zip", BodyParam.extensionForContentType("application/x-zip-compressed"))
        assertEquals(".json", BodyParam.extensionForContentType("application/json"))
        assertEquals(".txt", BodyParam.extensionForContentType("text/plain"))
        assertEquals(".bin", BodyParam.extensionForContentType("application/octet-stream"))
        assertEquals(".bin", BodyParam.extensionForContentType("something/unknown"))
    }


    // array properties: one part per element, all with the same name

    private fun arrayField(name: String, values: List<String>): ArrayGene<StringGene> {
        val arr = ArrayGene(name, StringGene(name))
        values.forEach { v -> arr.addElement(str(name, v)) }
        return arr
    }

    @Test
    fun testArrayPropertyBecomesOnePartPerElement() {
        val b = body(
            mapOf("attachments" to PartMeta(isFile = true, contentType = "image/png")),
            listOf(arrayField("attachments", listOf("AAA", "BBB", "CCC")))
        )
        val out = render(b)

        assertEquals(3, Regex("name=\"attachments\"").findAll(out).count(), out)

        assertFalse(out.contains("["), out)
        assertFalse(out.contains("AAA, BBB"), out)

        listOf("AAA", "BBB", "CCC").forEach {
            assertTrue(out.contains("$it$CRLF"), "missing value $it in $out")
        }
    }

    @Test
    fun testRepeatedPartsGetDistinctFilenamesFromTheirIndex() {
        val b = body(
            mapOf("attachments" to PartMeta(isFile = true, contentType = "application/pdf")),
            listOf(arrayField("attachments", listOf("A", "B")))
        )
        val out = render(b)

        //same name, so the index keeps filenames distinct
        assertTrue(out.contains("name=\"attachments\"; filename=\"attachments_0.pdf\""), out)
        assertTrue(out.contains("name=\"attachments\"; filename=\"attachments_1.pdf\""), out)
    }

    @Test
    fun testPlainFilePartKeepsItsUnsuffixedFilename() {
        //no index suffix outside arrays
        assertTrue(render(simpleBody()).contains("filename=\"file.png\""))
    }

    @Test
    fun testRepeatedPartsShareTheDeclaredMetadata() {
        val b = body(
            mapOf("docs" to PartMeta(isFile = true, contentType = "text/csv")),
            listOf(arrayField("docs", listOf("A", "B")))
        )
        val out = render(b)

        //encoding is per property, so all elements share the content type
        assertEquals(2, Regex("Content-Type: text/csv").findAll(out).count(), out)
    }

    @Test
    fun testEmptyArrayPropertyProducesNoPart() {
        val b = body(
            mapOf(
                "title" to PartMeta(isFile = false),
                "attachments" to PartMeta(isFile = true)
            ),
            listOf(str("title", "x"), arrayField("attachments", listOf()))
        )
        val out = render(b)

        assertFalse(out.contains("name=\"attachments\""), out)
        assertTrue(out.contains("name=\"title\""), out)
    }

    @Test
    fun testArrayOfNonFilePartsIsStillExpanded() {
        val b = body(
            mapOf("tags" to PartMeta(isFile = false)),
            listOf(arrayField("tags", listOf("red", "blue")))
        )
        val out = render(b)

        assertEquals(2, Regex("name=\"tags\"").findAll(out).count(), out)
        //not a file, so none of the repeated parts carries a filename
        assertFalse(out.contains("filename"), out)
    }

    @Test
    fun testArrayOfObjectsBecomesJsonParts() {
        val arr = ArrayGene("items", ObjectGene("items", listOf(str("k", ""))))
        arr.addElement(ObjectGene("items", listOf(str("k", "v1"))))
        arr.addElement(ObjectGene("items", listOf(str("k", "v2"))))

        val out = render(body(mapOf("items" to PartMeta(isFile = false)), listOf(arr)))

        assertEquals(2, Regex("Content-Type: application/json").findAll(out).count(), out)
        assertTrue(out.contains("v1") && out.contains("v2"), out)
    }

    @Test
    fun testBoundaryCollisionIsDetectedInsideARepeatedPart() {
        val b = body(
            mapOf("files" to PartMeta(isFile = true)),
            listOf(arrayField("files", listOf("ok", "zz${BodyParam.DEFAULT_BOUNDARY}zz")))
        )

        //collision check looks into each element
        assertEquals("${BodyParam.DEFAULT_BOUNDARY}2", b.multipartBoundary())
        assertTrue(render(b).startsWith("--${BodyParam.DEFAULT_BOUNDARY}2$CRLF"))
    }
}
