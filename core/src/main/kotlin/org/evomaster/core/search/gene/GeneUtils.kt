package org.evomaster.core.search.gene


object GeneUtils {

    /**
     * Given a number [x], return its string representation, with padded 0s
     * to have a defined [length]
     */
    fun padded(x: Int, length: Int) : String {

        require(length >= 0){"Negative length"}

        val s = x.toString()

        require(length >= s.length){"Value is too large for chosen length"}

        return if(x >=0 ){
            s.padStart(length, '0')
        } else {
            "-${(-x).toString().padStart(length-1, '0')}"
        }
    }

}