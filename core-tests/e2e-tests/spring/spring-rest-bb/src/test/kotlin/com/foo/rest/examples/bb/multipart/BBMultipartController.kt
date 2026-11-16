package com.foo.rest.examples.bb.multipart

import com.foo.rest.examples.bb.SpringController
import org.springframework.boot.SpringApplication

class BBMultipartController : SpringController(BBMultipartApplication::class.java) {

    /**
     * Same as the base class, plus the multipart settings.
     *
     * They matter: Spring Boot defaults `spring.servlet.multipart.file-size-threshold` to 0B, which
     * makes the servlet container write EVERY file part to a temp file on disk before the controller
     * method sees it. Our parts are a handful of bytes, so a threshold above that keeps them in memory
     * and the whole test leaves nothing behind on the file system.
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
