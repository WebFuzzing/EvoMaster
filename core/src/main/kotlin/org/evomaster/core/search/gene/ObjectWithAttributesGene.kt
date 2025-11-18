package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene

class ObjectWithAttributesGene(
    name: String,
    fixedFields: List<Gene>,
    refType: String? = null,
    isFixed: Boolean,
    template: PairGene<StringGene, Gene>? = null,
    additionalFields: MutableList<PairGene<StringGene, Gene>>? = null,
    val attributeNames: Set<String> = emptySet()
) : ObjectGene(name, fixedFields, refType, isFixed, template, additionalFields) {

    constructor(name: String, fields: List<Gene>, refType: String? = null) : this(
        name, fixedFields = fields, refType = refType, isFixed = true, template = null, additionalFields = null, attributeNames = emptySet()
    )

    override fun copyContent(): Gene {
        val copiedAdditional = additionalFields
            ?.map { it.copy() }
            ?.filterIsInstance<PairGene<StringGene, Gene>>()
            ?.toMutableList()

        return ObjectWithAttributesGene(
            name,
            fixedFields.map { it.copy() },
            refType,
            isFixed,
            template,
            copiedAdditional,
            attributeNames.toMutableSet()
        )
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {

        if (mode != GeneUtils.EscapeMode.XML) {
            return super.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        }

        val includedFields = fixedFields
            .filter { it !is CycleObjectGene }
            .filter { it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene) }
            .filter { it.isPrintable() }

        val attributeFields = includedFields.filter { attributeNames.contains(it.name) }
        val childFields = includedFields.filter { !attributeNames.contains(it.name) }

        // 1) "#text" CANNOT be an attribute
        if (attributeFields.any { it.name == "#text" }) {
            throw IllegalStateException("#text cannot be used as an attribute in XML")
        }

        // 2) Child names must be unique (XML does not allow repeated element names at this level)
        val duplicated = childFields
            .groupBy { it.name }
            .filter { it.value.size > 1 }
            .keys

        if (duplicated.isNotEmpty()) {
            throw IllegalStateException("Duplicate child elements not allowed in XML: $duplicated")
        }

        fun xmlEscape(s: String): String =
            s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")

        fun printAttribute(field: Gene): String {
            val raw = field.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.XML, targetFormat)
            val clean = raw.removeSurrounding("\"")
            return "${field.name}=\"$clean\""
        }

        val attributesString = attributeFields.joinToString(" ") { printAttribute(it) }
        val sb = StringBuilder()

        if (attributesString.isEmpty()) { //No childs
            sb.append("<$name>")
        } else {
            sb.append("<$name $attributesString>")
        }

        for (child in childFields) { //Childs

            val childXml = child.getValueAsPrintableString(
                previousGenes,
                GeneUtils.EscapeMode.XML,
                targetFormat
            )

            val isInlineValue = child.name == "#text" && !(child is ObjectWithAttributesGene)

            if (isInlineValue) {
                sb.append(childXml)
                continue
            }

            if (child is ObjectWithAttributesGene || child is ObjectGene) {
                sb.append(childXml)
                continue
            }

            sb.append("<${child.name}>")
            sb.append(childXml)
            sb.append("</${child.name}>")
        }

        sb.append("</$name>")
        return sb.toString()
    }
}