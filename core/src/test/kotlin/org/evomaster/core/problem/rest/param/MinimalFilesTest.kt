package org.evomaster.core.problem.rest.param

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

class MinimalFilesTest {

    private fun bytes(contentType: String) = MinimalFiles.files[contentType]!!.toByteArray(StandardCharsets.ISO_8859_1)

    @Test
    fun testImagesCanBeDecoded() {
        listOf(MinimalFiles.PNG, MinimalFiles.JPEG, MinimalFiles.GIF).forEach {
            val image = ImageIO.read(ByteArrayInputStream(bytes(it)))
            assertNotNull(image, it)
            assertEquals(1, image.width)
            assertEquals(1, image.height)
        }
    }

    @Test
    fun testPdfIsAsciiWithValidXref() {
        val pdf = MinimalFiles.files[MinimalFiles.PDF]!!
        assertTrue(pdf.all { it.code < 0x80 })
        assertTrue(pdf.startsWith("%PDF-"))
        assertTrue(pdf.trimEnd().endsWith("%%EOF"))

        //each object offset in the xref table must point to its "k 0 obj"
        val offsets = Regex("(\\d{10}) 00000 n").findAll(pdf).map { it.groupValues[1].toInt() }.toList()
        assertEquals(3, offsets.size)
        offsets.forEachIndexed { i, o -> assertTrue(pdf.startsWith("${i + 1} 0 obj", o)) }

        val startxref = pdf.substringAfter("startxref\n").substringBefore("\n").toInt()
        assertTrue(pdf.startsWith("xref", startxref))
    }

    @Test
    fun testWebpHeader() {
        val b = bytes(MinimalFiles.WEBP)
        assertEquals("RIFF", String(b, 0, 4, StandardCharsets.ISO_8859_1))
        assertEquals("WEBP", String(b, 8, 4, StandardCharsets.ISO_8859_1))
        val size = (b[4].toInt() and 0xFF) or ((b[5].toInt() and 0xFF) shl 8)
        assertEquals(b.size - 8, size)
    }

    @Test
    fun testZipIsAnEmptyArchive() {
        val b = bytes(MinimalFiles.ZIP)
        assertEquals("PK", String(b, 0, 2, StandardCharsets.ISO_8859_1))
        assertNull(ZipInputStream(ByteArrayInputStream(b)).nextEntry)
    }

    @Test
    fun testFilesForDeclaredContentTypes() {
        val f = MinimalFiles.files
        assertEquals(listOf(f[MinimalFiles.JPEG]), MinimalFiles.forContentTypes(listOf("image/jpeg")))
        assertEquals(listOf(f[MinimalFiles.JPEG]), MinimalFiles.forContentTypes(listOf("image/jpg")))
        assertEquals(listOf(f[MinimalFiles.PNG], f[MinimalFiles.GIF]),
            MinimalFiles.forContentTypes(listOf("image/png", "image/gif; charset=x")))
        assertEquals(listOf(f[MinimalFiles.ZIP]),
            MinimalFiles.forContentTypes(listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
        //text files are left to the search (and taint analysis)
        assertTrue(MinimalFiles.forContentTypes(listOf("text/csv")).isEmpty())
        //nothing declared, so any of them
        assertEquals(f.size, MinimalFiles.forContentTypes(listOf()).size)
    }

    @Test
    fun testContentTypeOf() {
        MinimalFiles.files.forEach { (type, file) -> assertEquals(type, MinimalFiles.contentTypeOf(file)) }
        assertNull(MinimalFiles.contentTypeOf("foo"))
    }
}
