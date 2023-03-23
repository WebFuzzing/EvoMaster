package org.evomaster.core.utils


/**
 * Class used to compute an average of values, but incrementally, each
 * time a new value is provided.
 * This is useful for when we deal with large amount of values (eg, hundreds
 * of thousands, or even millions), which would otherwise lead to a few
 * memory problems.
 *
 * Also keep track of other stats, like min and max.
 *
 * Can also handle timers
 */
class IncrementalAverage {

    /**
     * The number [n] of total values
     */
    var n : Long = 0
        private set

    /**
     * The average value.
     * For simplicity, considering 0 if no elements, ie [n==0]
     */
    var mean : Double = 0.0
        private set

    var min : Double = 0.0
        private set

    var max : Double = 0.0
        private set

    private var startingTime : Long? = null

    /**
     * add a new value to compute the incremental average.
     * this will be converted into a double
     */
    fun addValue(k: Number){

        val d = k.toDouble()

        if(n == 0L){
            min = d
            max = d
        } else {
            if(d < min){
                min = d
            }
            if(d > max){
                max = d
            }
        }

        n++

        //see https://math.stackexchange.com/questions/106700/incremental-averageing
        mean = mean + ((d - mean) / n.toDouble())
    }

    fun doStartTimer(){
        startingTime = System.currentTimeMillis()
    }

    fun isRecordingTimer() = startingTime != null

    /**
     * Add number of ms since timer was started, and then reset it
     */
    fun addElapsedTime() : Long{
        if(startingTime == null){
            throw IllegalStateException("Adding elapsed time before starting the timer")
        }
        val elapsed = System.currentTimeMillis() - startingTime!!
        addValue(elapsed)
        startingTime = null
        return elapsed
    }

    override fun toString() : String {
        return "Avg=%.2f , min=%.2f , max=%.2f".format(mean, min, max)
    }
}