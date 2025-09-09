package org.evomaster.core.search.gene.utils

import org.apache.commons.text.StringEscapeUtils
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.StaticCounter
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.*
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.wrapper.*
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.pow

object GeneUtils {

    private val log: Logger = LoggerFactory.getLogger(GeneUtils::class.java)


    /**
     * List where each element at position "i" has value "2^i"
     */
    val intpow2 = (0..30).map { 2.0.pow(it).toInt() }


    /**
     * @return whether the gene contains inactive optional gene
     */
    fun isInactiveOptionalGene(gene: Gene): Boolean{
        val optional = gene.getWrappedGene(OptionalGene::class.java)?:return false

        return !optional.isActive
    }

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
        EJSON,
        JSON,
        TEXT,
        XML,
        BODY,
        NONE,
        X_WWW_FORM_URLENCODED,
        BOOLEAN_SELECTION_MODE,
        BOOLEAN_SELECTION_NESTED_MODE,
        GQL_INPUT_MODE,
        GQL_INPUT_ARRAY_MODE,
        BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE,
        BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE,
        GQL_STR_VALUE,
        GQL_NONE_MODE
    }

    fun getDelta(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            range: Long = Long.MAX_VALUE,
            start: Int = intpow2.size,
            end: Int = 10
    ): Int {

        if (range < 1) {
            throw IllegalArgumentException("cannot generate delta with the range ($range) which is less than 1")
        }

        val maxIndex = apc.getExploratoryValue(start, end)

        var n = 0
        for (i in 0 until (maxIndex - 1)) {
            n = i + 1
            // check with Andrea regarding n instead of i
            if (intpow2[n] > range) {
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

        if (log.isTraceEnabled) {
            log.trace("repair genes {}", genes.joinToString(",") {
                //note that check whether the gene is printable is not enough here
                try {
                    it.getValueAsRawString()
                } catch (e: Exception) {
                    "null"
                }
            })
        }

        genes.forEach { it.repair() }
        // repair() will need to be refactored
//        for (g in genes) {
//            when (g) {
//                /*
//                    TODO, check with Andrea, why only DateGene and TimeGene?
//                    there also exist a repair for StringGene
//                 */
//                is DateGene, is TimeGene -> g.repair()
//            }
//        }
    }


    /**
     * [applyEscapes] - applies various escapes needed for assertion generation.
     * Moved here to allow extension to other purposes (SQL escapes, for example) and to
     * allow a more consistent way of making changes.
     *
     * This includes escaping special chars for java and kotlin.
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
            EscapeMode.JSON, EscapeMode.EJSON-> applyJsonEscapes(string, format)
            EscapeMode.TEXT -> applyTextEscapes(string, format)
            EscapeMode.NONE,
            EscapeMode.X_WWW_FORM_URLENCODED,
            EscapeMode.BOOLEAN_SELECTION_MODE,
            EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE,
            EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE,
            EscapeMode.BOOLEAN_SELECTION_NESTED_MODE,
            EscapeMode.GQL_NONE_MODE,
            EscapeMode.GQL_INPUT_ARRAY_MODE,
            EscapeMode.GQL_INPUT_MODE -> string

            EscapeMode.GQL_STR_VALUE -> applyGQLStr(string, format)
            EscapeMode.BODY -> applyBodyEscapes(string, format)
            EscapeMode.XML -> StringEscapeUtils.escapeXml10(string)
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

    /**
     * TODO might need a further handling based on [format]
     * Note that there is kind of post handling for graphQL, see [GraphQLUtils.getPrintableInputGenes]
     */
    private fun applyGQLStr(string: String, format: OutputFormat): String {
        val replace = string
                .replace("\"", "\\\\\"")

        return replace
    }

    private fun applyExpectationEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4): String {
        val ret = string.replace("\\", """\\\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")

        when {
            format.isKotlin() -> return ret.replace("\$", "\\\$")
            else -> return ret
        }
    }

    private fun applyUriEscapes(string: String, format: OutputFormat): String {
        //val ret = URLEncoder.encode(string, "utf-8")
        val ret = string.replace("\\", "%5C")
                .replace("\"", "%22")
                .replace("\n", "%0A")

        if (format.isKotlin()) return ret.replace("\$", "%24")
        else return ret
    }

    private fun applyTextEscapes(string: String, format: OutputFormat): String {
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

    private fun applyAssertionEscapes(string: String, format: OutputFormat): String {

        /*
            FIXME
            This was completely broken, as modifying the string for flakiness handling has
            nothing to do with applying escapes... which broke assertion generation for when
            we do full matches (and checking substrings).

            Flakiness handling has to be handled somewhere else. plus this is misleading, as
            eg messing up assertions on email addresses.
         */
//        var ret = ""
//        val timeRegEx = "[0-2]?[0-9]:[0-5][0-9]".toRegex()
//        ret = string.split("@")[0] //first split off any reference that might differ between runs
//                .split(timeRegEx)[0] //split off anything after specific timestamps that might differ

        val ret = string.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
        else return ret
    }

    private fun applyBodyEscapes(string: String, format: OutputFormat): String {
        val ret = string.replace("\\", """\\""")
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

    private fun applySqlEscapes(string: String, format: OutputFormat): String {
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
     *
     * [force] if true, throw exception if cannot prevent the cyclces
     */
    fun preventCycles(gene: Gene, force: Boolean = false) {
        preventSelectionOfGeneType(gene, CycleObjectGene::class.java, force)
    }

    fun tryToPreventSelection(gene: Gene): Boolean {
        var p = gene.parent

        loop@ while (p != null) {
            when (p) {
                //TODO possibly include ChoiceGene here?
                is SelectableWrapperGene -> {
                    p.forbidSelection()
                    break@loop
                }

                is ArrayGene<*> -> {
                    p.forceToOnlyEmpty()
                    break@loop
                }

                else -> p = p.parent
            }
        }

        return p != null
    }


    /**
     * When building a graph/tree of objects with fields, we might want to put a limit on the depth
     * of such tree.
     * In these cases, then LimitObjectGene will be created as place-holder to stop the recursion when
     * building the tree.
     *
     * Here, we make sure to LimitObjectGene are not reachable, in the same way as when dealing with
     * cycles.
     */
    fun preventLimit(gene: Gene, force: Boolean = false) {
        preventSelectionOfGeneType(gene, LimitObjectGene::class.java, force)
    }

    private fun preventSelectionOfGeneType(root: Gene, type: Class<out Gene>, force: Boolean = false) {

        val toExclude = root.flatView().filterIsInstance(type)
        if (toExclude.isEmpty()) {
            //nothing to do
            return
        }

        for (c in toExclude) {

            val prevented = tryToPreventSelection(c)

            if (!prevented) {
                val msg = "Could not prevent skipping gene in ${root.name} gene of type $type"
                if (force) {
                    throw RuntimeException(msg)
                }
                log.warn(msg)
            }
        }
    }


    /**
     * Check if there is any cycle, and, if so, if it has been prevented by deactivating something
     * in its ancestors.
     * If this returns true, then we have a major problem, as it would mean a Cycle object ends up
     * in the phenotype, breaking things.
     */
    fun hasNonHandledCycles(gene: Gene): Boolean {

        val cycles = gene.flatView().filterIsInstance<CycleObjectGene>()
        if (cycles.isEmpty()) {
            return false
        }

        for (c in cycles) {

            var p = c.parent
            loop@ while (p != null) {
                when {
                    /*
                        TODO possibly include ChoiceGene here?
                     */
                    (p is SelectableWrapperGene && !p.selectable) ||
                            (p is ArrayGene<*> && p.maxSize == 0)
                    -> {
                        break@loop
                    }

                    else -> p = p.parent
                }
            }

            if (p == null) {
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


    /**
     * In some cases, in particular GraphQL, given an object we might want to specify
     * just which fields we want to have, which is a boolean selection (ie, either a filed should
     * be present, or not). But we need to handle this recursively, because an object could have
     * objects inside, and so on recursively.
     *
     * However, to be able to print such selection for GraphQL, we need then to have a special mode
     * for its string representation.
     *
     * Also, we need to deal for when elements are non-nullable vs. nullable.
     */
    fun getBooleanSelection(gene: Gene): ObjectGene {

        if (shouldApplyBooleanSelection(gene)) {
            val selectedGene = handleBooleanSelection(gene)
            return if (selectedGene is OptionalGene) {
                selectedGene.gene as ObjectGene
            } else {
                selectedGene as ObjectGene
            }
        }
        throw IllegalArgumentException("Invalid input type: ${gene.javaClass}")
    }

    /**
     * force at least one boolean to be selected.
     * This is essential because, when making queries in which we ask for Object info, at
     * least 1 field MUST be selected. Otherwise, the query would not be syntactically valid.
     *
     * @return [true] if it was possible to make sure at least one field is selected.
     *          If not, the caller of this function MUST guarantee that this object is NOT used
     *          in the phenotype.
     *          Note: it can happen that some objects are not "usable" when we put limits to the
     *          depth of the queried trees.
     */
    fun repairBooleanSelection(obj: ObjectGene): Boolean {

        if (obj.fields.isEmpty()
            || obj.fields.count { it !is OptionalGene && it !is BooleanGene && it !is TupleGene } > 0
        ) {//this will include optional tuple
            throw IllegalArgumentException("There should be at least 1 field, and they must be all optional or boolean or tuple")
        }

        /*
        * Filtering fields that are already on, and that would imply we have nothing to do.
        */
        val selected = obj.fields.filter {
            ((it is OptionalGene && it.isActive) ||
                    (it is BooleanGene && it.value) ||
                    (it is OptionalGene && it.isActive  && it.gene is TupleGene && it.gene.lastElementTreatedSpecially && isLastSelected(
                        it.gene
                    )) ||
                    (it is TupleGene && it.lastElementTreatedSpecially && isLastSelected(it))
                    )
        }

        var failedRepairCount = 0

        /*
        * There are some fields that are already on.
        * However, we must guarantee that their children are fine as well, recursively.
        * If they are not fine, we need to deactivate them.
        * However, might end up we deactivate everything...
        */
        if (selected.isNotEmpty()) {
            //it is fine, but we still need to make sure selected objects are fine
            selected.forEach {
                if ((it is OptionalGene)) {
                    if (it.gene is ObjectGene) {
                        if (!repairBooleanSelection(it.gene)) {
                            it.isActive = false
                            failedRepairCount++
                        }
                } else if (it.gene is TupleGene && isLastElementInTupleObjetNotCycleNotLimit(it.gene)) {
                    if (!repairBooleanSelection(it.gene.elements.last() as ObjectGene)) {
                            failedRepairCount++
                        }
                    }
                } else
                    if (it is TupleGene && isLastElementInTupleObjetNotCycleNotLimit(it)){
                        if (!repairBooleanSelection(it.elements.last() as ObjectGene)) {
                            failedRepairCount++
                        }
                    }
            }
        }

        if (selected.isEmpty() || selected.size == failedRepairCount) {
            /*
             * we did not find fields that are already selected, or we ended up deselecting all of them.
             * must select at least one among the others that were off
             */
            val candidates = obj.fields.filter {
                (it is OptionalGene && it.selectable && !it.isActive)
                        || (it is BooleanGene && !it.value)
                        || (it is TupleGene && it.lastElementTreatedSpecially && isLastCandidate(it))
            }

            /*
            * What if we do not have candidates? we could not recycle any gene !.
            * In this case we must deactivate the ancestor of the gene (somehow, ex: put the optional as non-active) and return false
            * */
            if (candidates.isEmpty()) {
                return false
            }

            for (selectedGene in candidates) {
                if (selectedGene is OptionalGene && selectedGene.selectable) {
                    selectedGene.isActive = true
                    if (selectedGene.gene is ObjectGene) {
                        if (!repairBooleanSelection(selectedGene.gene)) {
                            selectedGene.isActive = false
                        }
                        return true //we just need one
                    } else if (selectedGene.gene is TupleGene){
                        var ok = true
                        if(selectedGene.gene.lastElementTreatedSpecially){
                            val lastElement = selectedGene.gene.elements.last()
                            ok = repairTupleLastElement(lastElement)
                        }
                        return ok
                    }
                } else if (selectedGene is TupleGene) {
                    var ok = true
                    if(selectedGene.lastElementTreatedSpecially) {
                        val lastElement = selectedGene.elements.last()
                        ok = repairTupleLastElement(lastElement)
                    }
                    return ok
                } else {
                    (selectedGene as BooleanGene).value = true
                    return true
                }
            }

            return false //we failed to fix any
        }

        //reached if we can repair the gene.
        return true
    }

    private fun repairTupleLastElement(lastElement: Gene): Boolean {
        if ((lastElement is ObjectGene) && (repairBooleanSelection(lastElement)))
            return true //we just need one
        else
            if (lastElement is BooleanGene) {
                lastElement.value = true
                return true
            }

        return false //should never be reached
    }

    private fun shouldApplyBooleanSelection(gene: Gene) =
            (gene is OptionalGene && gene.gene is ObjectGene)
                    || gene is ObjectGene
                    || (gene is ArrayGene<*> && gene.template is ObjectGene)
                    || (gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.gene is ObjectGene)
                    || (gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.gene is ObjectGene)
                    || (gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is ObjectGene)

    private fun handleBooleanSelection(gene: Gene): Gene {

        return when (gene) {
            is OptionalGene -> {
                /*
                    this is nullable.
                    Any basic field will be represented with a BooleanGene (selected/unselected).
                    But for objects we need to use an Optional
                 */
                if (gene.gene is ObjectGene)
                    OptionalGene(gene.name, handleBooleanSelection(gene.gene))
                else
                    if (gene.gene is ArrayGene<*>)
                        handleBooleanSelection(gene.gene.template)
                    else
                        if (gene.gene is TupleGene && gene.gene.lastElementTreatedSpecially) //opt tuple
                                //putting the tuple into optional gene
                                OptionalGene(gene.name, handleBooleanSelection(gene.gene))
                         else if (gene.gene is TupleGene) gene//since it contains only inputs
                        else if (gene.gene is LimitObjectGene) gene
                        else if (gene.gene is CycleObjectGene) gene
                        else
                        // on by default, but can be deselected during the search
                            BooleanGene(gene.name, true)
            }

            is CycleObjectGene -> {
                gene
            }

            is LimitObjectGene -> {
                gene
            }

            is ObjectGene -> {
                //need to look at each field
                ObjectGene(gene.name, gene.fields.map { handleBooleanSelection(it) })
            }

            is ArrayGene<*> -> handleBooleanSelection(gene.template)
            is TupleGene -> {//not opt tuple
                if (gene.lastElementTreatedSpecially)
                    //returning a tuple, with the last element handled by the Boolean selection
                    TupleGene(
                            gene.name,
                            gene.elements.dropLast(1).plus(handleBooleanSelection(gene.elements.last())),
                            lastElementTreatedSpecially = true
                    ) else gene // contains only inputs
            }

            else -> {
                BooleanGene(gene.name, true)
            }
        }
    }

    fun isGraphQLModes(mode: EscapeMode?) = mode == EscapeMode.BOOLEAN_SELECTION_MODE ||
            mode == EscapeMode.BOOLEAN_SELECTION_NESTED_MODE ||
            mode == EscapeMode.GQL_INPUT_MODE ||
            mode == EscapeMode.GQL_INPUT_ARRAY_MODE ||
            mode == EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE ||
            mode == EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE ||
            mode == EscapeMode.GQL_STR_VALUE

    private fun isLastSelected(gene: TupleGene): Boolean {
        val lastElement = gene.elements[gene.elements.size - 1]
        return (lastElement is OptionalGene && lastElement.isActive) ||
                (lastElement is BooleanGene && lastElement.value)||
                (lastElement is ObjectGene)

    }

    private fun isLastCandidate(gene: TupleGene): Boolean {
        val lastElement = gene.elements[gene.elements.size - 1]
        return (lastElement is OptionalGene && lastElement.selectable) || (lastElement is BooleanGene)

    }

    private fun isLastElementInTupleObjetNotCycleNotLimit(gene: TupleGene): Boolean {
        return (gene.lastElementTreatedSpecially &&
                 gene.elements.last() is ObjectGene &&
                ( (gene.elements.last() !is CycleObjectGene)||(gene.elements.last() !is LimitObjectGene))
        )
    }

    /**
     * A special string used for representing a place in the string
     * where we should add a SINGLE APOSTROPHE (').
     * This is used mainly for SQL value handling.
     */
    const val SINGLE_APOSTROPHE_PLACEHOLDER = "SINGLE_APOSTROPHE_PLACEHOLDER"

    private val QUOTATION_MARK = "\""

    /**
     * Returns a new string by removing the enclosing quotation marks of a string.
     * For example,
     * ""Hello World"" -> "Hello World"
     * """" -> ""
     * If the input string does not start and end with a
     * quotation mark, the output string is equal to the input string.
     * For example:
     * "Hello World"" -> "Hello World""
     * ""Hello World" -> ""Hello World"
     */
    fun removeEnclosedQuotationMarks(str: String): String {
        return if (str.startsWith(QUOTATION_MARK) && str.endsWith(QUOTATION_MARK)) {
            str.substring(1, str.length - 1)
        } else {
            str
        }
    }

    private fun encloseWithSingleApostrophePlaceHolder(str: String) = SINGLE_APOSTROPHE_PLACEHOLDER + str + SINGLE_APOSTROPHE_PLACEHOLDER

    /**
     * If the input string is enclosed in Quotation Marks, these symbols are replaced
     * by the special string in SINGLE_APOSTROPHE_PLACEHOLDER in the output string.
     * For example:
     * ""Hello"" -> "SINGLE_APOSTROPHE_PLACEHOLDERHelloSINGLE_APOSTROPHE_PLACEHOLDER".
     *
     * If the input string is not enclosed in quotation marks, the output string is equal
     * to the input string (i.e. no changes).
     */
    fun replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder(str: String): String {
        return if (str.startsWith(QUOTATION_MARK) && str.endsWith(QUOTATION_MARK)) {
            encloseWithSingleApostrophePlaceHolder(removeEnclosedQuotationMarks(str))
        } else {
            str
        }
    }




    fun getBasicGeneBasedOnJavaTypeBytecodeName(typeName: String, geneName: String) : Gene?{

        val valueType = if(typeName.startsWith("L") && typeName.endsWith(";")){
            typeName.substring(1, typeName.length - 1)
        } else {
            typeName
        }

        if(!valueType.startsWith("java")){
            /*
                not part of JDK... would a deserializer for a Map do that?
                eg, maybe using a library class? still feel weird, would need to double-check if this happens
             */
            log.warn("Not supported gene type for non-JDK class: $typeName")
            return null
        }

        val className = valueType.replace("/",".")
        val type = try{
            this::class.java.classLoader.loadClass(className)
        } catch (e: ClassNotFoundException) {
            log.warn("Unable to load class $className when inferring type for valueType $valueType")
            return null
        }

        return getBasicGeneBasedOnJavaType(type, geneName)
    }

    fun getBasicGeneBasedOnJavaType(type: Class<*>, name: String): Gene? {

        /*
            Note we could end up dealing with Number when having code like:

            INVOKEINTERFACE java/util/List.get (I)Ljava/lang/Object; (itf)
            CHECKCAST java/lang/Number
            INVOKEVIRTUAL java/lang/Number.intValue ()I

            in this case, just using an Int should "hopefully" be fine, regardless of which xValue() is called afterwards
         */

        return when{
            String::class.java.isAssignableFrom(type)
                    || java.lang.String::class.java.isAssignableFrom(type) -> StringGene(name)
            Integer::class.java.isAssignableFrom(type)
                    || java.lang.Integer::class.java.isAssignableFrom(type)
                    || type == Integer.TYPE-> IntegerGene(name)
            Double::class.java.isAssignableFrom(type)
                    || java.lang.Double::class.java.isAssignableFrom(type)
                    || type == java.lang.Double.TYPE-> DoubleGene(name)
            Boolean::class.java.isAssignableFrom(type)
                    || java.lang.Boolean::class.java.isAssignableFrom(type)
                    || type == java.lang.Boolean.TYPE-> BooleanGene(name)
            Float::class.java.isAssignableFrom(type)
                    || java.lang.Float::class.java.isAssignableFrom(type)
                    || type == java.lang.Float.TYPE-> FloatGene(name)
            Long::class.java.isAssignableFrom(type)
                    || java.lang.Long::class.java.isAssignableFrom(type)
                    || type == java.lang.Long.TYPE-> LongGene(name)
            Short::class.java.isAssignableFrom(type)
                    || java.lang.Short::class.java.isAssignableFrom(type)
                    || type == java.lang.Short.TYPE -> instantiateShortGene(name)
            Byte::class.java.isAssignableFrom(type)
                    || java.lang.Byte::class.java.isAssignableFrom(type)
                    || type == java.lang.Byte.TYPE-> instantiateByteGene(name)
            Char::class.java.isAssignableFrom(type)
                    || java.lang.Character::class.java.isAssignableFrom(type)
                    || type == java.lang.Character.TYPE -> instantiateCharGene(name)
            Map::class.java.isAssignableFrom(type)
                    || java.util.Map::class.java.isAssignableFrom(type) ->
                        TaintedMapGene(name, TaintInputName.getTaintName(StaticCounter.getAndIncrease()))
            List::class.java.isAssignableFrom(type)
                    || java.util.List::class.java.isAssignableFrom(type)
                    || Set::class.java.isAssignableFrom(type)
                    || java.util.Set::class.java.isAssignableFrom(type) ->
                        TaintedArrayGene(name, TaintInputName.getTaintName(StaticCounter.getAndIncrease()))
            //important this is last, as it matches all its subclasses
            Number::class.java.isAssignableFrom(type)
                    || java.lang.Number::class.java.isAssignableFrom(type)-> IntegerGene(name)
            else -> {
                null
            }
        }
    }

    fun getBasicGeneBasedOnBytecodeType(type: String, name: String) : Gene?{
        /*
        https://lambdaurora.dev/tutorials/java/bytecode/types.html
        V, represents void, it exists as a type descriptor for method return types.
        Z, represents a boolean type.
        B, represents a byte type.
        C, represents a char type.
        S, represents a short type.
        I, represents an int type.
        J, represents a long type.
        F, represents a float type.
        D, represents a double type.

        Objects -> L...;  eg Ljava/lang/String;
        */

        return when(type){
            "Z" -> BooleanGene(name)
            "B" -> instantiateByteGene(name)
            "C" -> instantiateCharGene(name)
            "S" -> instantiateShortGene(name)
            "I" -> IntegerGene(name)
            "J" -> LongGene(name)
            "F" -> FloatGene(name)
            "D" -> DoubleGene(name)
            else -> when{
                type.startsWith("[") -> instantiateArrayGeneBasedOnBytecodeType(type,name)
                else ->   getBasicGeneBasedOnJavaTypeBytecodeName(type, name)
            }
        }
    }

    fun instantiateArrayGeneBasedOnBytecodeType(type: String, name: String) : Gene?{
        if(!type.startsWith("[")){
            throw IllegalArgumentException("Array type not starting with [ -> $type")
        }
        val subtype = type.substring(1)

        if(subtype.contains("java/lang/Object")){
            return TaintedArrayGene(name, TaintInputName.getTaintName(StaticCounter.getAndIncrease()))
        }

        val template = getBasicGeneBasedOnBytecodeType(subtype, name)
        if(template == null){
            log.warn("Failed to instantiate gene for subtype: $subtype")
            return null
        }

        return ArrayGene(name, template)
    }

    fun instantiateByteGene(name: String) =
        IntegerGene(name, min = Byte.MIN_VALUE.toInt(), max = Byte.MAX_VALUE.toInt())

    fun instantiateCharGene(name: String) =
        StringGene(name, minLength = 1, maxLength = 1)

    fun instantiateShortGene(name: String) =
        IntegerGene(name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt())


    /**
     * Return all the "fields" of type string in the params.
     * The returned genes are not necessarily of type StringGene, as they could be wrapped (eg, in an OptionalGene).
     * If objects and arrays are encountered, those are analyzed recursively.
     */
    fun getAllStringFields(params: List<Param>) = getAllFields(params, StringGene::class.java)


    fun <K:Gene> getAllFields(params: List<Param>, klass: Class<K>) : List<Gene>{

        return params.flatMap { p ->
            if(p is HeaderParam || p is QueryParam || p is BodyParam){
                getAllFields(p.primaryGene(), klass)
            } else {
                // PathParam are explicitly excluded, as not really representing possible fields
                listOf()
            }
        }
    }

    /**
     * Check the leaf of this gene, and return this gene in case the leaf is matching [klass].
     * If the leaf is an array or an object, this process is applied recursively on their content.
     */
    fun <K: Gene> getAllFields(gene: Gene, klass: Class<K>) : List<Gene>{

        val fields = mutableListOf<Gene>()

        val leaf = gene.getLeafGene()

        if(klass.isAssignableFrom(leaf.javaClass)){
            //we are adding the wrapper gene, not the leaf
            fields.add(gene)
        }

        if(leaf is ObjectGene){
            leaf.fields.forEach {
                fields.addAll(getAllFields(it, klass))
            }
        }

        if(leaf is ArrayGene<*> && !leaf.isEmpty()){
            leaf.getViewOfElements().forEach {
                fields.addAll(getAllFields(it, klass))
            }
        }

        return fields
    }

}
