package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import kotlin.math.pow
import org.apache.commons.lang3.StringEscapeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GeneUtils {

    private val log: Logger = LoggerFactory.getLogger(GeneUtils::class.java)


    /**
     * List where each element at position "i" has value "2^i"
     */
    private val intpow2 = (0..30).map { 2.0.pow(it).toInt() }


    /**
     * The [EscapeMode] enum is here to clarify the supported types of Escape modes.
     *
     * Different purposes require different modes of escape (e.g. URI may require percent encoding). This is to
     * keep track of what modes are supported and how they map to the respective implementations.
     *
     * Any mode that is not supported will go under NONE, and will result in no escapes being applied at all. The
     * purpose is to ensure that, even if the mode being used is unsupported, the system will not throw an exception.
     * It may not behave as desired, but it should not crash.
     *
     */
    enum class EscapeMode {

        URI,
        SQL,
        ASSERTION,
        EXPECTATION,
        JSON,
        TEXT,
        XML,
        BODY,
        NONE,
        X_WWW_FORM_URLENCODED
    }

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

    fun applyEscapes(string: String, mode: EscapeMode = EscapeMode.NONE, format: OutputFormat): String {
        val ret = when (mode) {
            EscapeMode.URI -> applyUriEscapes(string, format)
            EscapeMode.SQL -> applySqlEscapes(string, format)
            EscapeMode.ASSERTION -> applyAssertionEscapes(string, format)
            EscapeMode.EXPECTATION -> applyExpectationEscapes(string, format)
            EscapeMode.JSON -> applyJsonEscapes(string, format)
            EscapeMode.TEXT -> applyTextEscapes(string, format)
            EscapeMode.NONE, EscapeMode.X_WWW_FORM_URLENCODED -> string
            EscapeMode.BODY -> applyBodyEscapes(string, format)
            EscapeMode.XML -> StringEscapeUtils.escapeXml(string)
        }
        //if(forQueries) return applyQueryEscapes(string, format)
        //else return applyAssertionEscapes(string, format)
        return ret
    }

    fun applyJsonEscapes(string: String, format: OutputFormat): String {
        val ret = string
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        return ret
    }

    fun applyExpectationEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4): String {
        val ret = string.replace("\\", """\\\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")

        when {
            format.isKotlin() -> return ret.replace("\$", "\\\$")
            else -> return ret
        }
    }

    fun applyUriEscapes(string: String, format: OutputFormat): String {
        //val ret = URLEncoder.encode(string, "utf-8")
        val ret = string.replace("\\", "%5C")
                .replace("\"", "%22")
                .replace("\n", "%0A")

        if (format.isKotlin()) return ret.replace("\$", "%24")
        else return ret
    }

    fun applyTextEscapes(string: String, format: OutputFormat): String {
        val ret = string.replace("\\", """\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        when {
            format.isKotlin() -> return ret.replace("\$", "\\\$")
            else -> return ret
        }

    }

    fun applyAssertionEscapes(string: String, format: OutputFormat): String {
        var ret = ""
        val timeRegEx = "[0-2]?[0-9]:[0-5][0-9]".toRegex()
        ret = string.split("@")[0] //first split off any reference that might differ between runs
                .split(timeRegEx)[0] //split off anything after specific timestamps that might differ
                .replace("\\", """\\\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
        else return ret
    }

    fun applyBodyEscapes(string: String, format: OutputFormat): String {
        var ret = string.replace("\\", """\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
                .replace("\\\\u", "\\u")
        //.replace("\$", "\${\'\$\'}")
        //ret.replace("\$", "\\\$")
        else return ret.replace("\\\\u", "\\u")

        /*
                   The \u denote unicode characters. For some reason, escaping the \\ leads to these being invalid.
                     Since they are valid in the back end (and they should, arguably, be possible), this leads to inconsistent behaviour.
                     This fix is a hack. It may be that some \u chars are not valid. E.g. \uAndSomeRubbish.

                     As far as I understand, the addition of an \ in the \unicode should not really happen.
                     They should be their own chars, and the .replace("\\", """\\""" should be fine, but for some reason
                     they are not.
                     */
    }

    fun applySqlEscapes(string: String, format: OutputFormat): String {
        val ret = string.replace("\\", """\\""")
                .replace("\"", "\\\\\"")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
        //.replace("\$", "\${\'\$\'}")
        //ret.replace("\$", "\\\$")
        else return ret
    }


    /**
     * Given an input gene, prevent any [CycleObjectGene] from affecting the phenotype.
     * For example, if [CycleObjectGene] is inside an [OptionalGene], then such gene
     * should never be selectable.
     * An array of [CycleObjectGene] would always be empty.
     * Etc.
     * However, it is not necessarily trivial. An [CycleObjectGene] might be required,
     * and so we would need to scan to its first ancestor in the tree which is an optional
     * or an array.
     */
    fun preventCycles(gene: Gene) {

        val cycles = gene.flatView().filterIsInstance<CycleObjectGene>()
        if (cycles.isEmpty()) {
            //nothing to do
            return
        }

        for (c in cycles) {

            var p = c.parent
            loop@ while (p != null) {
                when (p) {
                    is OptionalGene -> {
                        p.forbidSelection(); break@loop
                    }
                    is ArrayGene<*> -> {
                        p.forceToOnlyEmpty(); break@loop
                    }
                    else -> p = p.parent
                }
            }

            if (p == null) {
                log.warn("Could not prevent cycle in ${gene.name} gene")
            }
        }
    }

    fun hasNonHandledCycles(gene: Gene): Boolean {

        val cycles = gene.flatView().filterIsInstance<CycleObjectGene>()
        if (cycles.isEmpty()) {
            return false
        }

        for (c in cycles) {

            var p = c.parent
            loop@ while (p != null) {
                when {
                    (p is OptionalGene && !p.selectable) ||
                            (p is ArrayGene<*> && p.maxSize == 0)
                    -> {
                        break@loop
                    }
                    else -> p = p.parent
                }
            }

            if(p==null) {
                return true
            }
        }

        return false
    }

    /**
     * If the input gene is a root of a tree of genes (ie, it contains inside other genes),
     * then verify that the top ancestor of each child and their children is indeed this root.
     * Note: this is just testing for an invariant
     */
    fun verifyRootInvariant(gene: Gene): Boolean {

        if (gene.parent != null) {
            //not a root
            return true
        }

        val all = gene.flatView()
        if (all.size == 1) {
            //no child
            return true
        }

        for (g in all) {
            val root = g.getRoot()
            if (root != gene) {
                return false
            }
        }

        return true
    }
}

