package org.evomaster.core.output.clustering

import org.evomaster.core.output.clustering.metrics.DistanceMetric

class DBSCANClusterer<V> {
    private var epsilon: Double = 1.0
    private var minimumNumberOfClusterMembers: Int = 2
    private lateinit var metric: DistanceMetric<V>
    private lateinit var inputValues: List<V>
    private var visitedPoints: HashSet<V> = HashSet()

    constructor(values: Collection<V> , epsilon: Double = 1.0, minimumMembers: Int = 2, metric: DistanceMetric<V>){
        setEpsilon(epsilon)
        setMinimumNrOfMembers(members = minimumMembers)
        setInputValues(values)
        setMetric(metric)
    }

    fun setMinimumNrOfMembers(members: Int){
        if(members > 1) this.minimumNumberOfClusterMembers = members
    }

    fun setEpsilon(epsilon: Double){
        if(epsilon > 0.0) this.epsilon = epsilon
    }

    fun setMetric(metric: DistanceMetric<V>){
        this.metric = metric
    }

    fun setInputValues(values: Collection<V>){
        if(values.isEmpty()){
            throw Exception("DBSCAN: The list of values appears to be empty")
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
        if(!::inputValues.isInitialized) throw Exception("DBSCAN: List of inputs has not been initialized")
        if(inputValues.isEmpty()) throw Exception("DBSCAN: List of inputs is empty")
        if(inputValues.size < 2) throw Exception("DBSCAN: clustering less than 2 values is problematic")
        if(epsilon < 0.0) throw Exception("DBSCAN: Maximum distance cannot be less than 0 (though I don't know how you got here, because it shouldn't be possible)")

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