package com.foo.rest.examples.spring.openapi.v3.security.oracledisable

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class OracleDisableApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(OracleDisableApplication::class.java, *args)
        }

        private val dataLeakage = mutableMapOf<Int, String>()

        private val dataForbidden = ConcurrentHashMap<Int, String>()
        private val counterForbidden = AtomicInteger(0)

        private val dataNotRecognized = mutableMapOf<Int, String>()

        fun reset(){
            dataLeakage.clear()
            dataForbidden.clear()
            dataNotRecognized.clear()
            counterForbidden.set(0)
        }
    }

    private fun checkAuthLeakage(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")
    private fun checkAuthForbiddenDelete(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")
    private fun checkAuthNotRecognized(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")

    //leakage
    @PutMapping(path = ["/leakage/{id}"])
    open fun putLeakage(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuthLeakage(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataLeakage.containsKey(id)){
            dataLeakage[id] = auth!!
            return ResponseEntity.status(201).build()
        }

        val source = dataLeakage.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.status(204).build()
    }

    @GetMapping(path = ["/leakage/{id}"])
    open fun getLeakage(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int): ResponseEntity<String> {

        if(!checkAuthLeakage(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataLeakage.containsKey(id)){
            // wrong, leaking non-existence. should return 403
            return ResponseEntity.status(404).build()
        }

        val source = dataLeakage.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.status(200).body(source)
    }
    // forbiddendelete
    @DeleteMapping(path = ["/forbiddendelete/{id}"])
    open fun deleteForbidden(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuthForbiddenDelete(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataForbidden.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = dataForbidden.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }
        dataForbidden.remove(id)
        return ResponseEntity.status(204).build()
    }


    @PutMapping(path = ["/forbiddendelete/{id}"])
    open fun putForbidden(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuthForbiddenDelete(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataForbidden.containsKey(id)){
            dataForbidden[id] = auth!!
            return ResponseEntity.status(201).build()
        }

        val source = dataForbidden.getValue(id)
        return ResponseEntity.status(204).build()
    }


    @PatchMapping(path = ["/forbiddendelete/{id}"])
    open fun patchForbidden(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuthForbiddenDelete(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataForbidden.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = dataForbidden.getValue(id)
        return ResponseEntity.status(204).build()
    }

    // NotRecognized

    @PutMapping(path = ["/notrecognized/{id}"])
    open fun put(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuthNotRecognized(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataNotRecognized.containsKey(id)){
            dataNotRecognized[id] = auth!!
            return ResponseEntity.status(201).build()
        }

        val source = dataNotRecognized.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.status(204).build()
    }

    @GetMapping(path = ["/notrecognized/{id}"])
    open fun get(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int): ResponseEntity<String> {

        if(!checkAuthNotRecognized(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!dataNotRecognized.containsKey(id)){
            return ResponseEntity.status(403).build()
        }

        val source = dataNotRecognized.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.status(200).body(source)
    }

    @PostMapping(path = ["/notrecognized/"])
    open fun onlyBar(@RequestHeader("Authorization") auth: String?): ResponseEntity<String> {

        if(auth == null || auth != "BAR"){
            // wrong, as FOO should be recognized, and possibly return 403 if no access
            return ResponseEntity.status(401).build()
        }

        return ResponseEntity.status(204).build()
    }
}
