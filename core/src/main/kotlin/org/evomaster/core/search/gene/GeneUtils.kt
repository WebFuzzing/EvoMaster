package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import kotlin.math.pow


object GeneUtils {

    /**
     * List where each element at position "i" has value "2^i"
     */
    private val intpow2 = (0..30).map { 2.0.pow(it).toInt() }

    fun getDelta(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            range: Long = Long.MAX_VALUE,
            start: Int = intpow2.size,
            end: Int = 10
    ): Int {
        val maxIndex = apc.getExploratoryValue(start, end)

        var n = 0
        for (i in 0 until maxIndex) {
            n = i + 1
            if (intpow2[i] > range) {
                break
            }
        }

        //choose an i for 2^i modification
        val delta = randomness.chooseUpTo(intpow2, n)

        return delta
    }

    /**
     * Given a number [x], return its string representation, with padded 0s
     * to have a defined [length]
     */
    fun padded(x: Int, length: Int): String {

        require(length >= 0) { "Negative length" }

        val s = x.toString()

        require(length >= s.length) { "Value is too large for chosen length" }

        return if (x >= 0) {
            s.padStart(length, '0')
        } else {
            "-${(-x).toString().padStart(length - 1, '0')}"
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
    fun repairGenes(genes: Collection<Gene>) {

        for (g in genes) {
            when (g) {
                is DateGene -> repairDateGene(g)
                is TimeGene -> repairTimeGene(g)
            }
        }
    }

    private fun repairDateGene(date: DateGene) {

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
            if (month.value == 2 && day.value > 28) {
                //for simplicity, let's not consider cases in which 29...
                day.value = 28
            } else if (day.value > 30 && (month.value.let { it == 11 || it == 4 || it == 6 || it == 9 })) {
                day.value = 30
            } else if (day.value > 31) {
                day.value = 31
            }
        }
    }

    private fun repairTimeGene(time: TimeGene) {

        time.run {
            if (hour.value < 0) {
                hour.value = 0
            } else if (hour.value > 23) {
                hour.value = 23
            }

            if (minute.value < 0) {
                minute.value = 0
            } else if (minute.value > 59) {
                minute.value = 59
            }

            if (second.value < 0) {
                second.value = 0
            } else if (second.value > 59) {
                second.value = 59
            }
        }
    }

    /**
        [applyEscapes] - applies various escapes needed for assertion generation.
     Moved here to allow extension to other purposes (SQL escapes, for example) and to
     allow a more consistent way of making changes.

     * This includes escaping special chars for java and kotlin.
     * Currently, Strings containing "@" are split, on the assumption (somewhat premature, admittedly) that
     * the symbol signifies an object reference (which would likely cause the assertion to fail).
     * TODO: Tests are needed to make sure this does not break.
     * Escapes may have to be applied differently between:
         * Java and Kotlin
         * calls and assertions

     */

    fun applyEscapes(string: String, mode: String = "none", format: OutputFormat = OutputFormat.KOTLIN_JUNIT_5): String{
        val ret = when (mode){
            "uris" -> applyUriEscapes(string, format)
            "queries" -> applyQueryEscapes(string, format)
            "assertions" -> applyAssertionEscapes(string, format)
            "json" -> applyJsonEscapes(string, format)
            "text" -> applyTextEscapes(string, format)
            else -> string
        }
        //if(forQueries) return applyQueryEscapes(string, format)
        //else return applyAssertionEscapes(string, format)
        return ret
    }

    fun applyJsonEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4):String{
        val ret = when{
            format.isKotlin() -> string.replace("\$", "\\\$")
            else -> string
        }
        return ret
                .replace("\\", """\\""")
                .replace("\"", "\\\"")


    }

    fun applyUriEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4):String{
        val ret = if(format.isKotlin()) string.replace("\$", "%24")
        else string
        return ret.replace("\\", "%5C")
    }

    fun applyTextEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4):String{
        val ret = when{
            format.isKotlin() -> string.replace("\$", "\\\$")
            else -> string
        }
        return ret
                .replace("\\", """\\""")
                .replace("\"", "\\\"")

    }

    fun applyAssertionEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4): String {
        var ret = ""
        val timeRegEx = "[0-2]?[0-9]:[0-5][0-9]".toRegex()
        ret = string.split("@")[0] //first split off any reference that might differ between runs
                .split(timeRegEx)[0] //split off anything after specific timestamps that might differ
                .replace("\\", """\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        if (format.isKotlin()) return ret.replace("\$", "\${\'\$\'}")
        //ret.replace("\$", "\\\$")
        else return ret
    }

    fun applyQueryEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4): String {
        val ret = string
                //.replace("""\"""", """\\\"""")
                //.replace("""\\""", """\\\\""")
                .replace("\\", """\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")



        if (format.isKotlin()) return ret.replace("\$", "%24")
        //ret.replace("\$", "\\\$")
        else return ret
    }

}