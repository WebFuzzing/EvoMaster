package com.foo.rest.examples.bb.multipart

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * SUT for the multipart/form-data e2e tests.
 *
 * The endpoints are chosen so that the known limitations of the feature are MEASURED rather than
 * assumed: one that only stores an upload (the case the feature bets on), one that filters on the
 * filename extension, one that actually parses the content, and one that refuses urlencoded.
 *
 * NOTHING here ever touches the file system. Uploads are inspected in memory only, via [MultipartFile.size]
 * and [MultipartFile.bytes]; there is no transferTo anywhere. Together with a file-size-threshold above
 * our payload sizes (see application.properties), that keeps the servlet container from spilling parts
 * to temp files as well.
 */
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbmp"])
@RestController
open class BBMultipartApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBMultipartApplication::class.java, *args)
        }

        private val ALLOWED_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "pdf")
    }

    /**
     * 1. Text parts only. No file at all, so it isolates the envelope itself: if this fails, the
     *    multipart body is malformed, independently of anything to do with filenames.
     */
    @PostMapping("/text", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun textOnly(
        @RequestPart("title") title: String,
        @RequestPart(value = "note", required = false) note: String?
    ): ResponseEntity<String> {

        if (title.isBlank()) {
            return ResponseEntity.badRequest().body("blank title")
        }

        CoveredTargets.cover("MP_TEXT_ONLY")
        return ResponseEntity.ok("text ok: ${title.length}")
    }

    /**
     * 2. Store-only upload: looks at the metadata of the part and never at its content.
     *    **This is the case the whole feature bets on**, so it must be reachable.
     */
    @PostMapping("/store", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun storeOnly(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {

        //binding alone proves the part carried a filename: without one, Spring would not inject a MultipartFile
        val name = file.originalFilename ?: return ResponseEntity.badRequest().body("no filename")

        CoveredTargets.cover("MP_STORE_ONLY")
        //metadata only. Deliberately NOT stored anywhere: no transferTo, no disk
        return ResponseEntity.ok("stored $name with ${file.size} bytes")
    }

    /**
     * 3. Extension whitelist, the cheap filter that most upload endpoints apply before looking at a
     *    single byte. Reachable only if the emitted filename carries a sensible extension, which is
     *    what deriving it from the part content type buys.
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = [Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            //this is what makes the derived filename extension be .png rather than the generic .bin
            encoding = [Encoding(name = "file", contentType = "image/png")]
        )]
    )
    @PostMapping("/whitelist", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun extensionWhitelist(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {

        val name = file.originalFilename ?: return ResponseEntity.badRequest().body("no filename")

        val dot = name.lastIndexOf('.')
        if (dot < 0) {
            CoveredTargets.cover("MP_NO_EXTENSION")
            return ResponseEntity.badRequest().body("no extension")
        }

        val ext = name.substring(dot + 1).lowercase()
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            CoveredTargets.cover("MP_EXTENSION_REJECTED")
            return ResponseEntity.badRequest().body("extension not allowed: $ext")
        }

        CoveredTargets.cover("MP_EXTENSION_ACCEPTED")
        return ResponseEntity.ok("accepted .$ext")
    }

    /**
     * 4. Parses the content. Expected NOT to be passed: the content of a part is arbitrary text, not a
     *    real image. Asserting the 4xx is what documents that limitation with evidence.
     */
    @PostMapping("/validate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun validateContent(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {

        val image = try {
            //in memory: the bytes of the part, never a File
            ImageIO.read(ByteArrayInputStream(file.bytes))
        } catch (e: Exception) {
            null
        }

        if (image == null) {
            CoveredTargets.cover("MP_CONTENT_INVALID")
            return ResponseEntity.badRequest().body("not a valid image")
        }

        CoveredTargets.cover("MP_CONTENT_VALID")
        return ResponseEntity.ok("image ${image.width}x${image.height}")
    }

    /**
     * 5. Accepts ONLY multipart. With urlencoded, Spring answers 415 before reaching this method, so a
     *    2xx here is the direct proof that a real multipart request was sent, and that the old
     *    substitution to urlencoded no longer applies.
     */
    @PostMapping("/multipart-only", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun multipartOnly(@RequestPart("title") title: String): ResponseEntity<String> {
        CoveredTargets.cover("MP_ONLY")
        return ResponseEntity.ok("multipart only ok")
    }
}
