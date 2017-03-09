package org.evomaster.experiments.carfast

import com.google.inject.Inject
import com.google.inject.name.Named
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Sampler


class ISFSampler @Inject constructor(@Named("className") className: String) : Sampler<ISFIndividual>() {

    init{

        val kl = this.javaClass.classLoader.loadClass(className)

        kl.declaredMethods.forEach { m ->
            val action = ISFAction(m.name, m.parameterTypes.map { p -> IntegerGene("foo") })
            actionCluster.put(action.methodName, action)
        }
    }

    override fun sampleAtRandom(): ISFIndividual {

        val action = sampleAction()

        return ISFIndividual(action)
    }

    fun sampleAction(): ISFAction{
        val action = randomness.choose(actionCluster).copy() as ISFAction
        action.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return action
    }
}