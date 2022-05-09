package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

object GeneSamplerForTests {

    val geneClasses: List<KClass<out Gene>> = loadAllGeneClasses()

    private fun loadAllGeneClasses(): List<KClass<out Gene>> {

        val genes = mutableListOf<KClass<out Gene>>()
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
                .filter { !it.endsWith("\$Companion") }
                .filter { !it.contains("$") }
                .forEach {
                    //println("Analyzing $it")
                    val c = try {
                        this.javaClass.classLoader.loadClass(it).kotlin
                    } catch (e: Exception) {
                        println("Failed to load class: ${e.message}")
                        throw e
                    }
                    val subclass: Boolean = try {
                        Gene::class.isSuperclassOf(c)
                    } catch (e: java.lang.UnsupportedOperationException) {
                        false
                    }
                    if (subclass) {
                        genes.add(c as KClass<out Gene>)
                    }
                }
        return genes
    }

    fun <T> sample(klass: KClass<T>, rand: Randomness): T where T : Gene {

        return when (klass) {
            /*
                Note that here we do NOT randomize the values of genes, but rather
                the (fixed) constraints
             */
            StringGene::class -> sampleStringGene(rand) as T
            ArrayGene::class -> sampleArrayGene(rand) as T
            Base64StringGene::class -> sampleBase64StringGene(rand) as T
            else -> throw IllegalStateException("No sampler for $klass")

            //TODO need for all Genes
            // when genes need input genes, we sample those at random as well
        }
    }

    fun sampleArrayGene(rand: Randomness): ArrayGene<*> {

        val selection = geneClasses // TODO might filter out some genes here
        val chosen = sample(rand.choose(selection), rand)

        return ArrayGene("rand array ${rand.nextInt()}", chosen)
    }

    fun sampleBase64StringGene(rand: Randomness): Base64StringGene{
        return Base64StringGene("rand Base64StringGene ${rand.nextInt()}")
    }

    fun sampleStringGene(rand: Randomness): StringGene {

        val min = rand.nextInt(0, 3)
        val max = min + rand.nextInt(20)

        return StringGene("rand string ${rand.nextInt()}", minLength = min, maxLength = max)
    }

}