package com.foo.rest.examples.spring.openapi.v3.multipart

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * SUT for the white-box multipart e2e.
 *
 * Unlike the black-box one, here the point is COVERAGE: the branches below are only reachable if the
 * multipart envelope is well formed, if the part carries a filename, and if that filename has the
 * extension derived from the declared encoding. Being white-box, EvoMaster sees those branches through
 * instrumentation and the search is guided towards them.
 *
 * Nothing touches the file system: uploads are inspected via size and bytes, in memory only.
 */
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/multipart"])
@RestController
open class MultipartApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MultipartApplication::class.java, *args)
        }
    }

    /**
     * Nested branches on the text parts, so that reaching the deepest one requires a well formed
     * envelope with several parts at once.
     */
    /*
        NOTE: the text parts must be declared with @RequestPart, not @RequestParam. Spring reads a
        non-file part as a request parameter either way, but springdoc documents a plain @RequestParam
        as a QUERY parameter, so it never appears as a property of the multipart body and EvoMaster
        cannot know it is a part at all.
     */
    @PostMapping("/form", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun form(
        @RequestPart("title") title: String,
        @RequestPart("note") note: String
    ): ResponseEntity<String> {

        /*
            The branches are on the mere presence and length of the parts, deliberately: what this
            endpoint has to prove is that a well formed envelope with SEVERAL parts arrives and gets
            parsed. Anything stronger (e.g. parsing a part as a number) is a constraint on the value of
            a StringGene, which is a different problem and not what is under test here.
         */
        if (title.isBlank()) {
            return ResponseEntity.badRequest().body("blank title")
        }
        if (title.length < 3) {
            return ResponseEntity.status(422).body("title too short")
        }
        if (note.isBlank()) {
            return ResponseEntity.status(422).body("blank note")
        }
        return ResponseEntity.ok("form ok: ${title.length}/${note.length}")
    }

    /**
     * The upload branch chain: a filename must be present, must have an extension, and that extension
     * must be the one the schema declares via `encoding`.
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = [Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = [Encoding(name = "file", contentType = "application/pdf")]
        )]
    )
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    open fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {

        val name = file.originalFilename
            ?: return ResponseEntity.badRequest().body("no filename")

        val dot = name.lastIndexOf('.')
        if (dot < 0) {
            return ResponseEntity.status(422).body("no extension")
        }

        if (name.substring(dot + 1).lowercase() != "pdf") {
            return ResponseEntity.status(415).body("only pdf")
        }

        if (file.size == 0L) {
            return ResponseEntity.status(422).body("empty")
        }

        return ResponseEntity.ok("uploaded ${file.size} bytes")
    }
}
