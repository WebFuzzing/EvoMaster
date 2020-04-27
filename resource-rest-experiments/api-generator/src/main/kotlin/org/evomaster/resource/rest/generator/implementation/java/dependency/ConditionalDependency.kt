package org.evomaster.resource.rest.generator.implementation.java.dependency

import org.evomaster.resource.rest.generator.model.CommonTypes

/**
 * created by manzh on 2019-12-20
 *
 * by default 'dependency among/between relationship' is treated as existences. for instance,
 * if A depends on B, B must be existence. In this class, we are going to implement the two kinds of
 * dependency that relies on the 'property' of resources.
 *
 * One is based on a specified condition of a property, e.g., if A depends on B, 1) B must be existence
 *                  and 2) A.value < B.value
 * Another is based on a global status of the resource that depends on, e.g., If A depends on B, 1) B must
 *                  be existence, and 2) number of B < 5, or number of B < number of A
 *
 */
object ConditionalDependency {


    fun generateDependency(type: String, proAs :List<String>, proBs : List<String>) : String{

        if(CommonTypes.values().none { it.name.equals(type, ignoreCase = true)})
            throw IllegalArgumentException("a type of properties involved for dependency should be same based on current implementation")

        if (type.equals(CommonTypes.INT.name, ignoreCase = true) || type.equals(CommonTypes.OBJ_INT.name, ignoreCase = true))
            return handlingNumeric(proAs, proBs)

        // only support numeric at the moment (TODO String)
        if (type.toUpperCase() != CommonTypes.STRING.name)
            throw IllegalArgumentException("only support numeric at the moment, but $type")

        TODO("NOT IMPLEMENTED")
    }

    /**
     * return a condition with java
     */
    private fun handlingNumeric(pAs : List<String>, pBs : List<String>) : String{
        val npa = pAs.size
        val npb = pBs.size

        //1:N -> when all pBs satisfy some conditions, one resource is created
        if(npa == 1){
            val pa = pAs.first()
            return if (npb == 1)
                equalWith("$pa", pBs.first())
            else
                oneToN(pa, pBs)
        }
        //N:1 -> when the pB satisfy some conditions, N resources are created
        if (npa > 1 && npb == 1){
            val pb = pBs.first()
            return oneToN(pb, pAs)

        }
        //N:M
        return nToM(pAs, pBs)
    }

    private fun oneToN(pa : String, pBs: List<String>) : String{
        val npb = pBs.size
        if(npb == 2)
            return withinRange(pa, pBs[0], pBs[1])

        val all = "new double[]{${pBs.joinToString(",") { "$it * 1.0" }}}"
        val medium = "Util.medium($all)"
        val avg = "Util.average($all)"
        return withinRange(pa, medium, avg)
    }

    private fun nToM(pAs : List<String>, pBs: List<String>) : String{

        val all = "new double[]{${pBs.joinToString(",") { "$it * 1.0" }}}"
        val medium = "Util.medium($all)"
        val avg = "Util.average($all)"

        val allA = "new double[]{${pAs.joinToString(",") { "$it * 1.0" }}}"
        val mediumA = "Util.medium($allA)"
        val avgA = "Util.average($allA)"

        return "$mediumA < $medium && $avgA < $avg"
    }

    private fun withinRange(x : String, a : String, b : String)="$x <= Math.max($a, $b) && $x >= Math.min($a, $b)"

    private fun equalWith(a : String, b : String) = "$a == $b"

}

