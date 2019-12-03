package org.evomaster.core.output

import com.google.gson.Gson
import jsat.DataSet
import jsat.SimpleDataSet
import jsat.clustering.OPTICS
import jsat.linear.distancemetrics.DistanceMetric
import org.nield.kotlinstatistics.dbScanCluster
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.apache.commons.math3.ml.clustering.*
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.evomaster.core.output.clustering.DBSCANClusterer
import org.evomaster.core.output.clustering.metrics.DistanceMetricAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.GeneUtils

object Clusterer {

    //TODO: BMR - this is just here to run and evaluate the clusterer. Will be refactored to
    // account for more clustering options soon.

    fun cluster(solution: Solution<RestIndividual>,
                epsilon: Double = 0.6): MutableList<MutableList<RestCallResult>>{
        val sol1 = solution.individuals.filter{
            it.evaluatedActions().any{ ac ->
                val code = (ac.result as RestCallResult).getStatusCode()
                if (code!=null) (code == 500)
                else false
            }
        }

        val cluterableActions = sol1.flatMap {
            it.evaluatedActions().map { ac ->
                (ac.result as RestCallResult) }
        }

        val clu = DBSCANClusterer<RestCallResult>(
                values = cluterableActions,
                epsilon = epsilon,
                minimumMembers = 2,
                metric = DistanceMetricAction()
        )

        val clusters = clu.performCLustering()

        // clustering appears okay

        return clusters
    }
}

/*
class Clusterer{
    fun cluster(){
        //cluster patients by age and white blood cell count
        val clusters =  patients.dbScanCluster(maximumRadius = 2.0,
                minPoints = 1,
                xSelector = { it.age.toDouble() },
                ySelector = { it.whiteBloodCellCount.toDouble() }
        )

        // print out the clusters
        clusters.forEachIndexed { index, item ->
            println("CENTROID: $index")
            item.points.forEach {
                println("\t$it")
            }
        }

        val clusters2 = ref.dbScanCluster(maximumRadius = 50.0,
                minPoints = 3,
                xSelector = {it.length.toDouble()},
                ySelector = {it.hashCode().toDouble()})

    }


    data class Patient(val firstName: String,
                       val lastName: String,
                       val gender: Gender,
                       val birthday: LocalDate,
                       val whiteBloodCellCount: Int)  {

        val age = ChronoUnit.YEARS.between(birthday, LocalDate.now())
    }

    val patients = listOf(
            Patient("John", "Simone", Gender.MALE, LocalDate.of(1989, 1, 7), 4500),
            Patient("Sarah", "Marley", Gender.FEMALE, LocalDate.of(1970, 2, 5), 6700),
            Patient("Jessica", "Arnold", Gender.FEMALE, LocalDate.of(1980, 3, 9), 3400),
            Patient("Sam", "Beasley", Gender.MALE, LocalDate.of(1981, 4, 17), 8800),
            Patient("Dan", "Forney", Gender.MALE, LocalDate.of(1985, 9, 13), 5400),
            Patient("Lauren", "Michaels", Gender.FEMALE, LocalDate.of(1975, 8, 21), 5000),
            Patient("Michael", "Erlich", Gender.MALE, LocalDate.of(1985, 12, 17), 4100),
            Patient("Jason", "Miles", Gender.MALE, LocalDate.of(1991, 11, 1), 3900),
            Patient("Rebekah", "Earley", Gender.FEMALE, LocalDate.of(1985, 2, 18), 4600),
            Patient("James", "Larson", Gender.MALE, LocalDate.of(1974, 4, 10), 5100),
            Patient("Dan", "Ulrech", Gender.MALE, LocalDate.of(1991, 7, 11), 6000),
            Patient("Heather", "Eisner", Gender.FEMALE, LocalDate.of(1994, 3, 6), 6000),
            Patient("Jasper", "Martin", Gender.MALE, LocalDate.of(1971, 7, 1), 6000)
    )

    val ref = listOf("The request was rejected because the URL contained a potentially malicious String \";\"",
            "Text '2019-00-02' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 0", "Text '2579-20-23' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 20",
            "Typen var 6911-15-9, men må være en av: [lov, forskrift].", "Typen var 6961-15-9, men må være en av: [lov, forskrift].",
            "Text '3076-5-40' could not be parsed at index 5",
            "PreparedStatementCallback; SQL [ INSERT INTO comment(\n  id,\n  work_id,\n  expr_date,\n  fragment_id,\n  status,\n  comment,\n  created_at,\n  created_by,\n  last_updated_at,\n  last_updated_by\n) VALUES (\n  ?,\n  ?,\n  ?,\n  ?,\n  'DRAFT',\n  ?,\n  ?,\n  ?,\n  ?,\n  ?\n)\nERROR: new row for relation \"comment\" violates check constraint \"fragment_id_must_contain_one_of_expected_fragment_types\"\n  Detail: Failing row contains (998078bd-11bf-4ca9-b6aa-a9e8098f9bcf, evomaster_9510_input, null, DRAFT, <Kommentar><Metadata/><Body><Kommentarer/></Body></Kommentar>, 2019-10-16 ")

    fun levenshtein(lhs : CharSequence, rhs : CharSequence) : Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i

            for (j in 1..lhsLength) {
                val editCost= if(lhs[j - 1] == rhs[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + editCost
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength]
    }

    fun longestCommonSubsequence(a: String, b: String): String {
        if (a.length > b.length) return longestCommonSubsequence(b, a)
        var res = ""
        for (ai in 0 until a.length) {
            for (len in a.length - ai downTo 1) {
                for (bi in 0 until b.length - len + 1) {
                    if (a.regionMatches(ai, b, bi,len) && len > res.length) {
                        res = a.substring(ai, ai + len)
                    }
                }
            }
        }
        return res
    }
    enum class Gender {
        MALE,
        FEMALE
    }

    fun changeOfOptics(solution: Solution<RestIndividual>){
        val msgs: MutableList<String> = solution.individuals.filter { ind ->
            ind.results.any { rez ->
                (rez as RestCallResult).hasErrorCode()
            }
        }.flatMap {ind ->
            ind.results.map { rez ->
                //(rez as RestCallResult).getBody()
                val rezContents = Gson().fromJson((rez as RestCallResult).getBody(), Map::class.java)?.get("message")
                if(rezContents != null) rezContents as String
                else ""
            }
        }.toMutableList<String>()

        //val whatSet = SimpleDataSet(msgs)
        //val clusters = optics.cluster(msgs)

        println(GeneUtils.applyEscapes(msgs.joinToString(separator = "\", \"",
                prefix = "{\"",
                postfix = "\"}"), GeneUtils.EscapeMode.TEXT, OutputFormat.JAVA_JUNIT_5))

    }
}

class LevDistance {
    @Override
    fun distance(p0: String, p1: String): Double {
        val lhsLength = p0.length
        val rhsLength = p1.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i

            for (j in 1..lhsLength) {
                val editCost= if(p0[j - 1] == p1[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + editCost
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength].toDouble()
    }
}
*/