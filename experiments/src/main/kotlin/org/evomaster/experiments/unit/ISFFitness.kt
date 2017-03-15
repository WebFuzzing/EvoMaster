package org.evomaster.experiments.unit

import com.google.inject.Inject
import com.google.inject.name.Named
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.DoubleGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.FitnessFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.Method


class ISFFitness @Inject constructor(@Named("className") className: String) : FitnessFunction<ISFIndividual>() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ISFFitness::class.java)
    }

    val cl: InstrumentingClassLoader = InstrumentingClassLoader("com.foo")
    val kl = cl.loadClass(className)!!

    override fun doCalculateCoverage(individual: ISFIndividual): EvaluatedIndividual<ISFIndividual> {

        val m = kl.declaredMethods.filter { m -> m.name == individual.action.methodName }.first()!!

        val latestOut = System.out

        try {
            ExecutionTracer.reset()
            System.setOut(PrintStream(ByteArrayOutputStream()))

            val parameters = individual.action.genes.map { g ->
                when(g){
                    is IntegerGene -> g.value
                    is DoubleGene -> g.value
                    else -> throw IllegalStateException()
                }
            }

            val ins = kl.newInstance()

            invoke(m, ins, parameters)

        } catch (e: Exception) {
            //should never happen, unless bug
            System.setOut(latestOut)
            log.error("Failed reflection call: $e")
        } finally {
            System.setOut(latestOut)
        }

        val fv = FitnessValue(1.0)
        ExecutionTracer.getInternalReferenceToObjectiveCoverage().forEach { k, v ->
            val id = ObjectiveRecorder.getMappedId(k)
            fv.updateTarget(id, v)
        }

        return EvaluatedIndividual(fv, individual.copy() as ISFIndividual, listOf(ActionResult()))
    }

    private fun invoke(m: Method, ins: Any, p: List<Any>){

        when(p.size){
            0 -> m.invoke(ins)
            1 -> m.invoke(ins, p[0])
            2 -> m.invoke(ins, p[0], p[1])
            3 -> m.invoke(ins, p[0], p[1], p[2])
            4 -> m.invoke(ins, p[0], p[1], p[2], p[3])
            5 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4])
            6 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5])
            7 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6])
            8 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7])
            9 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8])
            10 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9])
            11 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10])
            12 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11])
            13 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12])
            14 -> m.invoke(ins, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12], p[13])
            else -> throw IllegalArgumentException("Cannot handle")
        }

    }
}