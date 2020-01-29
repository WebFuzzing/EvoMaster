package org.evomaster.core.output.clustering

import org.evomaster.core.output.clustering.metrics.DistanceMetric

/**
 * Implementation of density-based clustering algorithm DBSCAN.
 *
 * Original Publication:
 * Ester, Martin; Kriegel, Hans-Peter; Sander, Jørg; Xu, Xiaowei (1996).
 * Simoudis, Evangelos; Han, Jiawei; Fayyad, Usama M., eds.
 * A density-based algorithm for discovering clusters in large spatial
 * databases with noise. Proceedings of the Second International Conference
 * on Knowledge Discovery and Data Mining (KDD-96). AAAI Press. pp. 226-231
 *
 * Based on the implementation for java by Christopher Frantz (https://github.com/chrfrantz/DBSCAN).
 *
 * Usage:
 * - Identify type of input values.
 * - Implement appropriate metric for those inputs (extending [DistanceMetric] abstract class,
 * see [DistanceMetricAction] and [LevenshteinDistance] as an example)
 * - Instantiate (see [Clusterer] for example).
 * - Invoke [performClustering]
 *
 * The parameters:
 * [V] - the type of objects to be clustered.
 * [minimumNumberOfClusterMembers] - DBSCAN filters out outliers, so it does not allow single
 * element clusters. The default value for this is [minimumNumberOfClusterMembers = 2], since
 * we want to have the possibility of small clusters, where appropriate.
 * [metric] - the type of metric being used. Must extend the [DistanceMetric] abstract class.
 * [inputValues] - the collection of objects to be clustered.
 * [epsilon] - the density neighbourhood of an element. To cluster error messages, we use a
 * normalized distance (see [DistanceMetricAction]). The default value is 0.5 - that is, if
 * half of the error message is the same, the Actions will be clustered together. The idea is
 * to allow messages that differ in the details (timestamps, object references, host and port,
 * etc.) but are otherwise similar to cluster together.
 *
 */

class DBSCANClusterer<V>(values: Collection<V>, epsilon: Double = 0.5, minimumMembers: Int = 2, metric: DistanceMetric<V>) {
    private var epsilon: Double = 1.0
    private var minimumNumberOfClusterMembers: Int = 2
    private lateinit var metric: DistanceMetric<V>
    private lateinit var inputValues: List<V>
    private var visitedPoints: HashSet<V> = HashSet()

    init {
        setEpsilon(epsilon)
        setMinimumNrOfMembers(members = minimumMembers)
        setInputValues(values)
        setMetric(metric)
    }

    fun setMinimumNrOfMembers(members: Int){
        if(members > 1) this.minimumNumberOfClusterMembers = members
        else throw IllegalArgumentException("Minimum number of members per cluster cannot be less than 2. This would result in each element being its own cluster, and therefore not terribly helpful.")
    }

    fun setEpsilon(epsilon: Double){
        if(epsilon > 0.0) this.epsilon = epsilon
        else throw IllegalArgumentException("Epsilon cannot be a negative number. Epsilon is the allowable distance between members of the same cluster. Distances in general should not be negative.")
    }

    fun setMetric(metric: DistanceMetric<V>){
        this.metric = metric
    }

    fun setInputValues(values: Collection<V>){
        if(values.isEmpty()){
            throw IllegalArgumentException("DBSCAN: The list of values appears to be empty")
        }
        this.inputValues = values.toList()
    }

    private fun getNeighbours(inputValue: V): MutableList<V>{
        val neighbours: MutableList<V> = mutableListOf()
        for (i in 0..inputValues.size-1){
            val candidate = inputValues.get(i)
            if(metric.calculateDistance(inputValue, candidate) <= epsilon){
                neighbours.add(candidate)
            }
        }
        return neighbours
    }

    private fun mergeRightToLeft(neighbours1: MutableList<V>, neighbours2: MutableList<V>): MutableList<V>{
        for ( i in 0..neighbours2.size-1){
            val temPt = neighbours2.get(i)
            if(!neighbours1.contains(temPt)) neighbours1.add(temPt)
        }
        return neighbours1
    }


    fun performCLustering(): MutableList<MutableList<V>>{
        if(!::inputValues.isInitialized) throw IllegalArgumentException("DBSCAN: List of inputs has not been initialized")
        if(inputValues.isEmpty()) throw IllegalArgumentException("DBSCAN: List of inputs is empty")
        if(inputValues.size < 2) throw IllegalArgumentException("DBSCAN: clustering less than 2 values is problematic")
        if(epsilon < 0.0) throw IllegalArgumentException("DBSCAN: Maximum distance cannot be less than 0 (though I don't know how you got here, because it shouldn't be possible)")

        val resultList = mutableListOf<MutableList<V>>()
        visitedPoints.clear()

        var neighbours: MutableList<V> = mutableListOf()

        var index = 0

        while (inputValues.size > index){
            val p = inputValues.get(index)
            if(!visitedPoints.contains(p)){
                visitedPoints.add(p)
                neighbours = getNeighbours(p)

                if(neighbours.size >= minimumNumberOfClusterMembers){
                    var ind = 0
                    while (neighbours.size > ind){
                        val r = neighbours.get(ind)
                        if(!visitedPoints.contains(r)){
                            visitedPoints.add(r)
                            val individualNeighbours = getNeighbours(r)
                            if(individualNeighbours.size >= minimumNumberOfClusterMembers){
                                neighbours = mergeRightToLeft(
                                        neighbours,
                                        individualNeighbours
                                )
                            }
                        }
                        ind++
                    }
                    resultList.add(neighbours)
                }
            }
            index++
        }
        return resultList
    }



}