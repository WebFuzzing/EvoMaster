package org.evomaster.core.problem.rest.param

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.Deflater
import javax.imageio.ImageIO

/**
 * Smallest valid file of each content type, to pass the checks APIs do on uploads (eg magic bytes, or decoding).
 * Each file is a Latin-1 String, ie one char per byte, as the multipart body is sent as ISO-8859-1.
 */
object MinimalFiles {

    const val PDF = "application/pdf"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
    const val GIF = "image/gif"
    const val WEBP = "image/webp"
    const val ZIP = "application/zip"

    /**
     * Content type to file. PDF is ASCII only, the others need the multipart body to be sent as bytes.
     */
    val files: Map<String, String> by lazy {
        listOfNotNull(
            PDF to pdf(),
            PNG to png(),
            jpeg()?.let { JPEG to it },
            GIF to fromHex("47494638396101000100800000ffffff00000021f90401000000002c00000000010001000002024401003b"),
            //1x1 lossless WebP
            WEBP to fromBytes(Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==")),
            //empty archive, ie only the end of central directory record
            ZIP to fromHex("504b0506" + "00".repeat(18))
        ).toMap()
    }

    /**
     * Files matching the content types declared for a part (eg in its `encoding`), or all if none is declared
     */
    fun forContentTypes(declared: List<String>): List<String> {
        if (declared.isEmpty()) {
            return files.values.toList()
        }
        return declared.mapNotNull { files[normalize(it)] }.distinct()
    }

    /**
     * Content type of [value] if it is one of the minimal files, null otherwise
     */
    fun contentTypeOf(value: String): String? = files.entries.firstOrNull { it.value == value }?.key

    private fun normalize(contentType: String): String {
        val t = contentType.substringBefore(";").trim().lowercase()
        return when {
            t == "image/jpg" || t == "image/pjpeg" -> JPEG
            //Office files are zip archives
            t.contains("zip") || t.contains("openxmlformats") -> ZIP
            else -> t
        }
    }

    private fun fromBytes(bytes: ByteArray) = String(bytes, StandardCharsets.ISO_8859_1)

    private fun fromHex(hex: String) = fromBytes(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())

    private fun pdf(): String {
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 1 1] >>"
        )
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = objects.mapIndexed { i, o ->
            val offset = sb.length
            sb.append("${i + 1} 0 obj\n$o\nendobj\n")
            offset
        }
        val xref = sb.length
        sb.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { sb.append(String.format("%010d 00000 n \n", it)) }
        sb.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return sb.toString()
    }

    private fun png(): String {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        //1x1, 8 bits, RGBA
        pngChunk(out, "IHDR", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0))
        //filter byte + 1 transparent pixel
        pngChunk(out, "IDAT", deflate(byteArrayOf(0, 0, 0, 0, 0)))
        pngChunk(out, "IEND", byteArrayOf())
        return fromBytes(out.toByteArray())
    }

    private fun pngChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(StandardCharsets.US_ASCII)
        out.write(int32(data.size))
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32().apply { update(typeBytes); update(data) }
        out.write(int32(crc.value.toInt()))
    }

    private fun int32(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data)
        deflater.finish()
        val buffer = ByteArray(64)
        val n = deflater.deflate(buffer)
        deflater.end()
        return buffer.copyOf(n)
    }

    private fun jpeg(): String? = try {
        val out = ByteArrayOutputStream()
        if (ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", out)) fromBytes(out.toByteArray()) else null
    } catch (e: Exception) {
        //eg, no image support in the JVM
        null
    }
}
