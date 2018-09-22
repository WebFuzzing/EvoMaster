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

    /**
     * When we generate data, we might want to generate invalid inputs
     * on purpose to stress out the SUT, ie for Robustness Testing.
     * But there are cases in which such kind of data makes no sense.
     * For example, when we initialize SQL data directly bypassing the SUT,
     * there is no point in having invalid data which will just make the SQL
     * commands fail with no effect.
     *
     * So, we simply "repair" such genes with only valid inputs.
     */
    fun repairGenes(genes: Collection<Gene>){

        for(g in genes){
            when(g){
                is DateGene -> repairDateGene(g)
                is TimeGene -> repairTimeGene(g)
            }
        }
    }

    private fun repairDateGene(date: DateGene){

        date.run {
            if (month.value < 1) {
                month.value = 1
            } else if (month.value > 12) {
                month.value = 12
            }

            if (day.value < 1) {
                day.value = 1
            }

            //February
            if (month.value == 2 && day.value > 28){
                //for simplicity, let's not consider cases in which 29...
                day.value = 28
            } else if(day.value > 30 && (month.value.let { it == 11 || it == 4 || it == 6 || it == 9} )){
                day.value = 30
            } else if(day.value > 31){
                day.value = 31
            }
        }
    }

    private fun repairTimeGene(time: TimeGene){

        time.run {
            if(hour.value < 0){
                hour.value = 0
            } else if(hour.value > 23){
                hour.value = 23
            }

            if(minute.value < 0){
                minute.value = 0
            } else if(minute.value > 59){
                minute.value = 59
            }

            if(second.value < 0){
                second.value = 0
            } else if(second.value > 59){
                second.value = 59
            }
        }
    }

}