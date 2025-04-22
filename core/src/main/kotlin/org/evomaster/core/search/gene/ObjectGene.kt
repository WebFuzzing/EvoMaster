package org.evomaster.core.search.gene

import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.graphql.GqlConst
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.root.CompositeConditionalFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.utils.GeneUtils.isInactiveOptionalGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder

/**
 * this is a gene which could have fixed fields [fixedFields] and more additional fields [additionalFields].
 * The additional fields need to follow the [template] and could have flexible type (eg, T is FlexibleGene)
 * eg, a schema example
 *  Person:
 *      type: object
 *      additionalProperties:
 *          oneOf:
 *              - type: string
 *              - type: integer
 */
class ObjectGene(
        name: String,
        val fixedFields: List<out Gene>,
        val refType: String? = null,
        /**
         * represent whether the Object is fixed
         * which determinate whether it allows to have additional fields
         */
        isFixed : Boolean,
        /**
         * a template for additionalFields
         */
        val template : PairGene<StringGene, Gene>?,
        /**
         * additional fields, and its field name is mutable
         *
         * note that [additionalFields] is not null only if [isFixed] is true
         */
        additionalFields:  MutableList<PairGene<StringGene, Gene>>?
): CompositeConditionalFixedGene(
        name, isFixed,
        mutableListOf<Gene>().apply { addAll(fixedFields); if (additionalFields!=null) addAll(additionalFields) })
{

    init {
        if (isFixed){
            if (template != null)
                throw IllegalArgumentException("cannot specify template when the ObjectGene is fixed")
            if (additionalFields != null)
                throw IllegalArgumentException("cannot specify additional field when the ObjectGene is fixed")
        }
    }

    constructor(name: String, fields: List<out Gene>, refType: String? = null) : this(name, fields, refType, true, null, null)

    companion object{
        private val log: Logger = LoggerFactory.getLogger(ObjectGene::class.java)
        // probability of mutating size of additional fields
        private const val PROB_MODIFY_SIZE_ADDITIONAL_FIELDS = 0.1
        // the default maximum size for additional fields
        private const val MAX_SIZE_ADDITIONAL_FIELDS = 5
    }

    val fields : List<out Gene>
        get() {return children}

    /**
     * @return a view of additional fields
     */
    val additionalFields: List<PairGene<StringGene, FlexibleGene>>?
        get() {
            if (isFixed) return null
            return children.filterNot { fixedFields.contains(it) }.filterIsInstance<PairGene<StringGene, FlexibleGene>>()
        }

    /*
        In theory, it is possible to have an object with no fields...
     */
    override fun canBeChildless() = true

    override fun copyContent(): Gene {
        return ObjectGene(name, fixedFields.map(Gene::copy), refType, isFixed, template, additionalFields?.map {it.copy() as PairGene<StringGene, Gene> }?.toMutableList())
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        fixedFields.filter { it.isMutable() }
                .forEach { it.randomize(randomness, tryToForceNewValue) }

        if (!isFixed){
            Lazy.assert {
                template != null && additionalFields != null
            }
            if (additionalFields!!.isNotEmpty())
                killChildren(additionalFields!!)
            val num = randomness.nextInt(MAX_SIZE_ADDITIONAL_FIELDS)
            repeat(num){
                val added = sampleElementToAdd(randomness)
                if (added != null){
                    addChild(added)
                }
            }
        }
    }

    override fun customShouldApplyShallowMutation(randomness: Randomness, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (isFixed) return false
        return randomness.nextBoolean(PROB_MODIFY_SIZE_ADDITIONAL_FIELDS)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        if (isFixed)
            throw IllegalStateException("shallowMutate is not applied to fixed ObjectGene")

        Lazy.assert {
            additionalFields != null
            template != null
        }

        if (additionalFields!!.isEmpty() || (additionalFields!!.size < MAX_SIZE_ADDITIONAL_FIELDS && randomness.nextBoolean(0.5))){
            val added = sampleElementToAdd(randomness)
            if (added != null){
                addChild(added)
                return true
            }


            if (additionalFields!!.isEmpty()){
                log.warn("fail to apply shallowMutate for FlexibleObject, i.e., adding or removing additional field")
                return false
            }
        }

        val remove = randomness.choose(additionalFields!!)
        killChild(remove)
        return true
    }

    /**
     * @return an element to add. if the element is null, it means that there is no element which can satisfy constraints of [this]
     */
    private fun sampleElementToAdd(randomness: Randomness) : PairGene<StringGene, FlexibleGene>?{
        if (isFixed)
            throw IllegalStateException("addElement should not applied when the ObjectGene is fixed")
        Lazy.assert {
            template != null && additionalFields != null
        }
        val copy = template!!.copy() as PairGene<StringGene, FlexibleGene>
        copy.randomize(randomness, false)

        if (existingKey(copy)){
            copy.randomize(randomness, false)
        }

        if(this.initialized){
            copy.markAllAsInitialized()
        }

        if (existingKey(copy))
            return null
        return copy
    }

    private fun existingKey(fieldToAdd: PairGene<StringGene, FlexibleGene>): Boolean{
        return additionalFields!!.any { it.first.value == fieldToAdd.first.value}
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (other.isFixed != isFixed)
            throw IllegalArgumentException("cannot copy value for ObjectGene if their isFixed is different")

        val updateOk = updateValueOnlyIfValid(
            {
                var ok = true

                for (i in fixedFields.indices) {
                    ok = ok && this.fixedFields[i].copyValueFrom(other.fixedFields[i])
                }
                ok
            }, true
        )

        if (!updateOk) return updateOk

        if (isFixed) return true

        if (!template!!.possiblySame(other.template!!))
            throw IllegalArgumentException("different template ${other.template.javaClass}")


        //TODO for additional fields

        return true
    }


    /**
     *This function takes as input a gene filter which contains fields to skip.
     * The output is an object gene without the fields of the filter gene.
     * It is used in the GQL interface type since we need to remove redundant fields in each object in the interface.
     * Important: Depending on where copyFields is invoked, it might miss the binding references.
     * But if it is used for creating genes, it would be ok.
     */
    fun copyFields(filterGene: ObjectGene): ObjectGene {

        if (!isFixed)
            throw IllegalArgumentException("cannot support copyFields when ObjectGene is not fixed")

        val fixedFields: MutableList<Gene> = mutableListOf()
        for (fld in this.fixedFields) {
            var exist = false
            for (element in filterGene.fields) {
                if (fld.name == element.name) {
                    exist = true
                    break
                }
            }
            if (!exist) {
                fixedFields.add(fld.copy())
            }
        }


        // TODO for additional fields
        return ObjectGene(this.name, fixedFields, refType, isFixed, null, null)
    }

    override fun isMutable(): Boolean {
        return getViewOfChildren().any { it.isMutable() }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (other.isFixed != isFixed) return false

        if (!isFixed && template!!.possiblySame(other.template!!))
            throw IllegalArgumentException("different template ${other.template.javaClass}")

        return this.fixedFields.size == other.fixedFields.size
                && (isFixed || additionalFields!!.size == other.additionalFields!!.size)
                && this.fixedFields.zip(other.fixedFields) { thisField, otherField -> thisField.containsSameValueAs(otherField) }.all { it }
                && (isFixed || this.additionalFields!!.zip(other.additionalFields!!) { thisField, otherField -> thisField.containsSameValueAs(otherField) }.all { it }
        )
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is ObjectGene
                && (fixedFields.indices).all { fixedFields[it].possiblySame(gene.fixedFields[it]) }
                && isFixed == gene.isFixed
                && (isFixed || template!!.possiblySame(gene.template!!))) {

            var result = true
            (fixedFields.indices).forEach {
                val r = fixedFields[it].setValueBasedOn(gene.fixedFields[it])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind the field ${fixedFields[it].name}")
                result = result && r
            }
            if (!result)
                LoggingUtil.uniqueWarn(log, "fail to fully bind field values with the ObjectGene")

            //TODO bind additional fields

            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind the ${this::class.java.simpleName} with ${gene::class.java.simpleName}")
        return false
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {

        if (additionalGeneMutationInfo.impact != null
                && additionalGeneMutationInfo.impact is ObjectGeneImpact) {
            val impacts = internalGenes.map { additionalGeneMutationInfo.impact.fixedFields.getValue(it.name) }
            val selected = mwc.selectSubGene(
                    internalGenes, true, additionalGeneMutationInfo.targets, individual = null, impacts = impacts, evi = additionalGeneMutationInfo.evi
            )
            val map = selected.map { internalGenes.indexOf(it) }
            return map.map { internalGenes[it] to additionalGeneMutationInfo.copyFoInnerGene(impact = impacts[it] as? GeneImpact, gene = internalGenes[it]) }
        }
        throw IllegalArgumentException("impact is null or not ObjectGeneImpact, ${additionalGeneMutationInfo.impact}")
    }


    override fun mutationWeight(): Double = fields.map { it.mutationWeight() }.sum()


    private fun shouldPrintAsJSON(mode: GeneUtils.EscapeMode?) : Boolean{
        //by default, return in JSON format. same wise if we have an undefined TEXT mode
        return mode == null || mode == GeneUtils.EscapeMode.JSON || mode == GeneUtils.EscapeMode.TEXT
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if (mode != null && !shouldPrintAsJSON(mode) && !isFixed)
            throw IllegalStateException("do not support getValueAsPrintableString with mode ($mode) for non-fixed ObjectGene")
        val buffer = StringBuffer()

        val includedFields = fields.filter {
            it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene))
        } .filter { it.isPrintable() }


        if (shouldPrintAsJSON(mode) || mode == GeneUtils.EscapeMode.EJSON) {
            buffer.append("{")

            includedFields.map {
                "\"${it.name}\":${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            }.joinTo(buffer, ", ")


            if (!isFixed){
                additionalFields!!.filter {
                    it.isPrintable() && !isInactiveOptionalGene(it)
                }.also {
                    if (it.isNotEmpty() && includedFields.isNotEmpty())
                        buffer.append(", ")
                }.joinTo(buffer, ", ") {
                    "\"${it.first.value}\":${it.second.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
                }
            }

            buffer.append("}")

        } else if (mode == GeneUtils.EscapeMode.XML) {

            // TODO might have to handle here: <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            /*
                Note: this is a very basic support, which should not really depend
                much on. Problem is that we would need to access to the XSD schema
                to decide when fields should be represented with tags or attributes
             */

            buffer.append(openXml(name))
            includedFields.forEach {
                //FIXME put back, but then update all broken tests
                //buffer.append(openXml(it.name))
                buffer.append(it.getValueAsPrintableString(previousGenes, mode, targetFormat))
                //buffer.append(closeXml(it.name))
            }
            buffer.append(closeXml(name))

        } else if (mode == GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED) {

            buffer.append(includedFields.map {
                val name = URLEncoder.encode(it.getVariableName(), "UTF-8")
                val value = URLEncoder.encode(it.getValueAsRawString(), "UTF-8")
                "$name=$value"
            }.joinToString("&"))

        } else if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE || mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE) {
            handleBooleanSelectionMode(includedFields, buffer, previousGenes, targetFormat, mode, extraCheck)
        } else if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE) {
            handleUnionObjectSelection(includedFields, buffer, previousGenes, targetFormat)
        } else if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE) {
            handleUnionFieldSelection(includedFields, buffer, previousGenes, targetFormat)
        } else if (mode == GeneUtils.EscapeMode.GQL_INPUT_MODE) {
            handleInputSelection(buffer, includedFields, previousGenes, mode, targetFormat)
        } else if (mode == GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE) {
            handleInputArraySelection(buffer, includedFields, previousGenes, mode, targetFormat)
        } else {
            throw IllegalArgumentException("Unrecognized mode: $mode")
        }

        return buffer.toString()
    }

    private fun handleInputArraySelection(buffer: StringBuffer, includedFields: List<Gene>, previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?) {
        //GQL array in arguments need a special object printing mode form that differ from Json, Boolean selection and gql input modes:
        //without the obj name:  {FieldNName: instance }
        buffer.append("{")
        includedFields.map {
            when {
                (it.getWrappedGene(EnumGene::class.java)!=null) -> "${it.name}:${it.getValueAsRawString()}"
                else -> "${it.name}:${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            }
        }.joinTo(buffer, ", ")
        buffer.append("}")
    }

    private fun handleInputSelection(buffer: StringBuffer, includedFields: List<Gene>, previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?) {
        //GQL arguments need a special object printing mode form that differ from Json and Boolean selection:
        //ObjName:{FieldNName: instance }
        buffer.append(name)
        buffer.append(":{")

        includedFields.map {
            //not opt or if opt, it should be active
            if ((it.getWrappedGene(OptionalGene::class.java) == null) || (it.getWrappedGene(OptionalGene::class.java)?.isActive == true))
                "${it.name}:${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            //opt not active
            else ""
        }.joinTo(buffer, ", ")

        buffer.append("}")
    }

    private fun handleUnionFieldSelection(
        includedFields: List<Gene>,
        buffer: StringBuffer,
        previousGenes: List<Gene>,
        targetFormat: OutputFormat?
    ) {
        /*For GraphQL we need UNION OBJECT FIELDS MODE to print out object`s fields in the union type eg:
               ... on UnionObject1 {
                fields<----
           }
               ... on UnionObjectN {
                fields<----
           }
       */
        val selection = selection(includedFields)

        buffer.append(selection.joinToString(",") {
            val s: String = when (it) {
                is TupleGene -> {
                    it.getValueAsPrintableString(
                        previousGenes,
                        GeneUtils.EscapeMode.GQL_NONE_MODE,
                        targetFormat,
                        extraCheck = true
                    )
                }

                is OptionalGene -> {
                    if (it.name.endsWith(GqlConst.INTERFACE_TAG))
                    //for the nested interfaces need the name of the field
                        it.gene.name.replace(GqlConst.INTERFACE_TAG, "") + it.gene.getValueAsPrintableString(
                            previousGenes,
                            GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE,
                            targetFormat
                        )
                    else
                        it.gene.getValueAsPrintableString(
                            previousGenes,
                            GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE,
                            targetFormat
                        )
                }

                is ObjectGene -> {//todo check
                    it.getValueAsPrintableString(
                        previousGenes,
                        GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE,
                        targetFormat
                    )
                }

                is BooleanGene -> {
                    it.name
                }

                else -> {
                    throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
                }
            }
            s
        })

    }

    private fun handleUnionObjectSelection(includedFields: List<Gene>, buffer: StringBuffer, previousGenes: List<Gene>, targetFormat: OutputFormat?) {
        /*For GraphQL we need UNION OBJECT MODE to print out the objects in the union type eg:
                ... on UnionObject1 {<----------
                fields
            }
                ... on UnionObjectN {<----------
                 fields
            }
             */
        val selection = includedFields.filter {
            when (it) {
                is OptionalGene -> it.isActive
                else -> throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
            }
        }
        selection.map {
            val s: String = when (it) {
                is OptionalGene -> {
                    if (it.name.endsWith(GqlConst.INTERFACE_BASE_TAG)) {
                        assert(it.gene is ObjectGene)
                        buffer.append("${it.gene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE, targetFormat)}").toString()
                    } else {
                        buffer.append("... on ${it.gene.name.replace(GqlConst.UNION_TAG, "")} {")
                        assert(it.gene is ObjectGene)
                        buffer.append("${it.gene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE, targetFormat)}")
                        buffer.append("}").toString()
                    }
                }

                else -> {
                    throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
                }
            }
            s
        }.joinToString()
    }

    private fun handleBooleanSelectionMode(includedFields: List<Gene>, buffer: StringBuffer, previousGenes: List<Gene>, targetFormat: OutputFormat?, mode: GeneUtils.EscapeMode?, extraCheck: Boolean) {

        //  if (includedFields.isEmpty()) {
        //      buffer.append("$name")
        //      return
        //  }

        if (name.endsWith(GqlConst.UNION_TAG)) {
            if (!extraCheck)
                buffer.append("{") else buffer.append("${name.replace(GqlConst.UNION_TAG, " ")} {")
            buffer.append(getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE, targetFormat, extraCheck = true))
            buffer.append("}")
            return
        }

        if (name.endsWith(GqlConst.INTERFACE_TAG)) {
            if (!extraCheck)
                buffer.append("{") else buffer.append("${name.replace(GqlConst.INTERFACE_TAG, " ")} {")
            buffer.append(getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE, targetFormat, extraCheck = true))
            buffer.append("}")
            return
        }

        if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE) {
            //we do not do it for the first object, but we must do it for all the nested ones
            buffer.append("$name")
        }

        if (!name.endsWith(GqlConst.INTERFACE_BASE_TAG)) {
            buffer.append("{")
        }

        val selection = selection(includedFields)

        buffer.append(selection.map {
            val s: String = when (it) {
                is TupleGene -> {
                    it.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.GQL_NONE_MODE, targetFormat, extraCheck = true)
                }
                is OptionalGene -> {
                    //could be an object or a tuple
                    it.gene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE, targetFormat, extraCheck = true)
                }
                is ObjectGene -> {
                    it.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE, targetFormat, extraCheck = true)
                }
                is BooleanGene -> {
                    it.name
                }
                else -> {
                    throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
                }
            }
            s
        }.joinToString(","))

        if (!name.endsWith(GqlConst.INTERFACE_BASE_TAG)) {
            buffer.append("}")
        }
    }

    private fun selection(includedFields: List<Gene>): List<Gene> {
        val selection = includedFields.filter {
            when (it) {
                is TupleGene -> true
                is OptionalGene -> it.isActive
                is ObjectGene -> true // TODO check if should skip if none of its subfield is selected
                is BooleanGene -> it.value
                else -> throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
            }
        }
        return selection
    }

    private fun openXml(tagName: String) = "<$tagName>"

    private fun closeXml(tagName: String) = "</$tagName>"



}