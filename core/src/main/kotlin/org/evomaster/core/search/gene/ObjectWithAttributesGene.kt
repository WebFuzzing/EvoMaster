package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.utils.CollectionUtils

/**
 * An extension of [ObjectGene] that supports XML attributes.
 *
 * While [ObjectGene] renders all its fields as child XML elements, this class distinguishes
 * between fields that should be rendered as XML attributes (inside the opening tag, e.g.
 * `<element attr="value">`) and fields that should remain as child elements.
 * The distinction is controlled by [attributeNames]: any field whose name appears in that set
 * is serialised as an XML attribute; all other fields are serialised as child elements.
 *
 * This class overrides [getValueAsPrintableString] to produce the correct XML output when
 * [GeneUtils.EscapeMode.XML] is requested, and overrides [containsSameValueAs] to take
 * attribute membership into account when comparing two genes.
 */
class ObjectWithAttributesGene(
    name: String,
    fixedFields: List<Gene>,
    refType: String? = null,
    isFixed: Boolean,
    template: PairGene<StringGene, Gene>? = null,
    additionalFields: MutableList<PairGene<StringGene, Gene>>? = null,
    /**
     * The set of field names (from [fixedFields]) that must be serialised as XML attributes
     * rather than as child elements.  Every name in this set must match the [Gene.name]
     * of one of the entries in [fixedFields]; an [IllegalArgumentException] is thrown at
     * construction time if any name in this set has no corresponding field.
     * The special name `"#text"` is reserved for element content and therefore cannot be an
     * attribute — an [IllegalArgumentException] is also thrown if it is present.
     */
    val attributeNames: Set<String> = emptySet()
) : ObjectGene(name, fixedFields, refType, isFixed, template, additionalFields) {

    init {
        // "#text" is reserved for element content and cannot be used as an attribute name
        if (attributeNames.contains("#text")) {
            throw IllegalArgumentException("Invalid XML schema: '#text' is reserved for element content and cannot be used as an attribute name.")
        }
        // Every attribute name must correspond to an existing field in fixedFields
        val fieldNames = fixedFields.map { it.name }.toSet()
        val unknown = attributeNames.filter { it !in fieldNames }
        if (unknown.isNotEmpty()) {
            throw IllegalArgumentException("Invalid XML schema: attribute name(s) $unknown are not defined in fixedFields.")
        }
    }

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
            attributeNames
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {

        if (other !is ObjectGene) {
            return false
        }

        // If other is also ObjectWithAttributesGene, attributeNames must match
        // If other is plain ObjectGene, this.attributeNames must be empty to produce same XML
        if (other is ObjectWithAttributesGene) {
            // != uses Set.equals(), which compares by content (same elements, any order),
            // not by reference — correct here since two independently built sets with the
            // same attribute names should be considered equivalent
            if (this.attributeNames != other.attributeNames) {
                return false
            }
        } else {
            // other is ObjectGene but not ObjectWithAttributesGene
            // They can only have same value if this has no attributes
            if (this.attributeNames.isNotEmpty()) {
                return false
            }
        }

        return super.containsSameValueAs(other)
    }

    private fun printAttribute(
        previousGenes: List<Gene>,
        targetFormat: OutputFormat?,
        field: Gene
    ): String {
        val raw = field.getValueAsPrintableString(
            previousGenes,
            GeneUtils.EscapeMode.XML,
            targetFormat
        )

        val clean = cleanXmlValueString(raw)
        return "${field.name}=\"$clean\""
    }

    private fun cleanXmlValueString(v: String): String =
        v.removeSurrounding("\"")

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

        val (attributeFields, childFields) = includedFields.partition { attributeNames.contains(it.name) }

        val attributesString = attributeFields.joinToString(" ") { printAttribute(previousGenes, targetFormat, it) }
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

            val isInlineValue = child.name == contentXMLTag && !(child is ObjectWithAttributesGene)

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