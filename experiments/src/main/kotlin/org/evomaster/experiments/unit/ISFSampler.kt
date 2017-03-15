package org.evomaster.experiments.unit

import com.google.inject.Inject
import com.google.inject.name.Named
import org.evomaster.core.search.gene.DoubleGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Sampler
import java.lang.reflect.Modifier


class ISFSampler @Inject constructor(@Named("className") className: String) : Sampler<ISFIndividual>() {

    init {

        val kl = this.javaClass.classLoader.loadClass(className)

        kl.declaredMethods
                .filter { m -> Modifier.isPublic(m.modifiers)}
                .forEach { m ->
                    val action = ISFAction(m.name, m.parameterTypes.map { p ->
                        when {
                            p.isAssignableFrom(Int::class.java) -> IntegerGene("foo")
                            p.isAssignableFrom(Double::class.java) -> DoubleGene("foo")
                            else -> throw IllegalStateException()
                        }
                    })
                    actionCluster.put(action.methodName, action)
                }
    }

    override fun sampleAtRandom(): ISFIndividual {

        val action = sampleAction()

        return ISFIndividual(action)
    }

    fun sampleAction(): ISFAction {
        val action = randomness.choose(actionCluster).copy() as ISFAction
        action.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return action
    }
}