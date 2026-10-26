package com.foo.rest.examples.spring.openapi.v3.multipart

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.springframework.boot.SpringApplication

class MultipartController : SpringController(MultipartApplication::class.java) {

    /**
     * Same as the base class, plus the multipart settings: Spring Boot defaults
     * `spring.servlet.multipart.file-size-threshold` to 0B, which makes the servlet container spill
     * every file part to a temp file on disk. Raising it above our payload sizes keeps everything in
     * memory, so the test leaves nothing behind.
     */
    override fun startSut(): String {
        ctx = SpringApplication.run(
            applicationClass,
            "--server.port=0",
            "--spring.servlet.multipart.file-size-threshold=1MB",
            "--spring.servlet.multipart.max-file-size=1MB",
            "--spring.servlet.multipart.max-request-size=2MB"
        )
        return "http://localhost:$sutPort"
    }
}
