package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.KClass

class GeneTest {


    companion object {

        private val genes: MutableList<KClass<out Gene>> = mutableListOf()

        @JvmStatic
        @BeforeAll
        fun init() {
            val target = File("target/classes")
            if (!target.exists()) {
                throw IllegalStateException("Compiled class folder does not exist: ${target.absolutePath}")
            }

            target.walk()
                    .filter { it.name.endsWith(".class") }
                    .map {
                        val s = it.path.replace("\\", "/")
                                .replace("target/classes/", "")
                                .replace("/", ".")
                        s.substring(0, s.length - ".class".length)
                    }
                    .filter { ! it.endsWith("\$Companion") }
                    .filter { !it.contains("$") }
                    .forEach {
                        //println("Analyzing $it")
                        val c = try {
                             this.javaClass.classLoader.loadClass(it).kotlin
                        }catch (e: Exception){
                            println("Failed to load class: ${e.message}")
                            throw e
                        }
                        val subclass : Boolean = try {
                            Gene::class.isSuperclassOf(c)
                        } catch (e: java.lang.UnsupportedOperationException){
                            false
                        }
                        if(subclass){
                            genes.add(c as KClass<out Gene>)
                        }
                    }
        }
    }

    @Test
    fun testNumberOfGenes(){
        /*
            This number should not change, unless you explicitly add/remove any gene.
            if so, update this number accordingly
         */
        assertEquals(64, genes.size)
    }

    @Test
    fun testPackage() {

        val wrongPackage = genes.map { it.qualifiedName!! }
                .filter { ! it.startsWith("org.evomaster.core.search.gene") }

        if(wrongPackage.size > 0){
            println("Wrong packages: $wrongPackage")
        }
        assertEquals(0, wrongPackage.size)
    }
}