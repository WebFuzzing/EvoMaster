package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
            /*
                Load all the classes that extends from Gene
             */
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

        val errors = genes.map { it.qualifiedName!! }
                .filter { ! it.startsWith("org.evomaster.core.search.gene") }

        if(errors.isNotEmpty()){
            println("Wrong packages: $errors")
        }
        assertEquals(0, errors.size)
    }

    @Test
    fun testNameSuffix(){

        val errors = genes.map { it.qualifiedName!! }
                .filter { ! it.endsWith("Gene") }

        if(errors.isNotEmpty()){
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }

    @Test
    fun testHierarchy(){

        val errors = genes.filter {
            it != Gene::class && !SimpleGene::class.isSuperclassOf(it) && !CompositeGene::class.isSuperclassOf(it)
        }

        if(errors.isNotEmpty()){
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }


    @Test
    fun testStringGene(){
        /*
            this kind of test could be moved directly into StringGeneTest, but here it is just to checkout
            if sampler is working fine
         */
        val rand = Randomness()
        for(i in 0..100){
            val s = GeneSamplerForTests.sample(StringGene::class, rand)
            s.randomize(rand, true, listOf())
            assertTrue(s.value.length >= s.minLength)
            assertTrue(s.value.length <= s.maxLength)
        }
    }

    @Test
    fun testCanSample(){

        val errors = genes.filter {
            try{GeneSamplerForTests.sample(it, Randomness()); false} catch (e: Exception){true}
        }

        if(errors.isNotEmpty()){
            println("Cannot sample: $errors")
        }
        assertEquals(0, errors.size)
    }

    private fun getSample(seed: Long) : List<Gene>{
        val rand = Randomness()
        rand.updateSeed(seed)

        return genes.map { GeneSamplerForTests.sample(it, Randomness()) }
    }

    @Test
    fun testRoot(){

        val sample = getSample(123)

        sample.forEach { root ->
            root.identifyAsRoot()
            assertTrue(root.isDefinedRoot())
            val wholeTree = root.flatView{it != root}
            assertTrue(wholeTree.all { ! it.isDefinedRoot() }) // only 1 root
        }
    }

    @Test
    fun testParent(){
        val sample = getSample(321)

        sample.forEach { root ->
            root.identifyAsRoot()
            val wholeTree = root.flatView{it != root}

            wholeTree.forEach { n ->
                var p = n
                while(p != null){p = p.parent as Gene}
                assertEquals(root, p)
            }
        }
    }

    //TODO for each *Gene, sample random instances, and verify properties
}