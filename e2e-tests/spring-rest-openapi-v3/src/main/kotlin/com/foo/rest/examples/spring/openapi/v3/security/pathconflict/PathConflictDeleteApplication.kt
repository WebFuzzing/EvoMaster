package com.foo.rest.examples.spring.openapi.v3.security.pathconflict

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
open class PathConflictDeleteApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PathConflictDeleteApplication::class.java, *args)
        }
        private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")

        // ID counters
        private val articleCounter = AtomicInteger(0)
        private val commentCounter = AtomicInteger(0)

        // Data stores
        private val articles = mutableMapOf<Int, String>()
        private val comments = mutableMapOf<Int, MutableMap<Int, String>>()

        fun reset() {
            articles.clear()
            comments.clear()
            articleCounter.set(0)
            commentCounter.set(0)
        }
    }

    @PostMapping("/api/articles")
    fun createArticle(@RequestHeader("Authorization") auth: String?): ResponseEntity<Int> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        val id = articleCounter.incrementAndGet()
        articles[id] = auth!!
        comments[id] = mutableMapOf()
        return ResponseEntity.status(201).header(HttpHeaders.LOCATION, "/api/articles/$id").build()
    }

    @GetMapping("/api/articles/{id}")
    fun getArticle(@RequestHeader("Authorization") auth: String?,
                   @PathVariable id: Int): ResponseEntity<String> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!articles.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = articles.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.status(200).body(source)
    }

    // Comment endpoints
    @PostMapping("/api/articles/{id}/comments")
    fun createComment(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable id: Int
    ): ResponseEntity<Int> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if (id !in articles) return ResponseEntity.status(404).build()

        val commentId = commentCounter.incrementAndGet()
        comments[id]?.put(commentId, auth!!)
        return ResponseEntity.status(201).header(HttpHeaders.LOCATION, "/api/articles/$id/comments/$commentId").build()
    }

    @GetMapping("/api/articles/{id}/comments/{commentId}")
    fun getComment(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable id: Int,
        @PathVariable commentId: Int
    ): ResponseEntity<String> {
        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!articles.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = articles.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        return comments[id]?.get(commentId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/api/articles/{id}")
    fun deleteArticle(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable id: Int
    ): ResponseEntity<String>{
        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }
        if(!articles.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = articles.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        articles.remove(id)
        comments.remove(id)
        return ResponseEntity.status(204).build()
    }

    @DeleteMapping("/api/articles/{id}/comments/{commentId}")
    fun deleteComment(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable id: Int,
        @PathVariable commentId: Int
    ): ResponseEntity<String>{
        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }
        if(!articles.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = articles.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        comments[id]?.remove(commentId)
        return ResponseEntity.status(204).build()
    }
}