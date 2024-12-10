package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse

import javax.validation.constraints.NotNull


/**
 * code is based on NotationConverter from genome_nexus https://github.com/genome-nexus/genome-nexus
 */
class GLocationDto {

    @NotNull
    var chromosome: String? = null

    @NotNull
    var start: Int? = null

    @NotNull
    var end: Int? = null

    var referenceAllele: String? = null
    var variantAllele: String? = null


    companion object{
        fun genomicToHgvs(dto: GLocationDto) : String?{
            val normal  = dto.normalization()
            val start = normal.start!!
            val end = normal.end!!
            val ref = normal.referenceAllele?:""
            val `var` = normal.variantAllele?:""
            val chr = normal.chromosome!!

            return if (start < 1) {
                null
            } else if (ref == "-" || ref.length == 0 || ref == "NA" || ref.contains("--")) {
                try {
                    chr + ":g." + start + "_" + (start + 1).toString() + "ins" + `var`
                } catch (e: NumberFormatException) {
                    return null
                }
            } else if (`var` == "-" || `var`.length == 0 || `var` == "NA" || `var`.contains("--")) {
                if (ref.length == 1) {
                    chr + ":g." + start + "del"
                } else {
                    chr + ":g." + start + "_" + end + "del"
                }
            } else if (ref.length > 1 && `var`.length >= 1) {
                chr + ":g." + start + "_" + end + "delins" + `var`
            } else if (ref.length == 1 && `var`.length > 1) {

                chr + ":g." + start + "delins" + `var`
            } else {
                chr + ":g." + start + ref + ">" + `var`
            }
        }
    }

    override fun toString(): String {
        return "${chromosome},${start},${end},${referenceAllele?:""},${variantAllele?:""}"
    }

    private fun normalization() : GLocationDto{

        val chr = chromosome!!.trim { it <= ' ' }.replace("chr", "").replace("23", "X").replace("24", "Y")

        var nstart: Int = start!!
        val nend: Int = end!!
        var nref: String = referenceAllele?.trim()?:""
        var nvar: String = variantAllele?.trim()?:""

        var prefix = ""

        if (nref != nvar) {
            prefix = longestCommonPrefix(nref, nvar)
        }

        if (prefix.isNotEmpty()) {
            nref = nref.substring(prefix.length)
            nvar = nvar.substring(prefix.length)
            nstart = start!! + prefix.length
            if (nref.isEmpty()) {
                nstart -= 1
            }
        }

        return GLocationDto().apply {
            this.chromosome= chr
            this.start = nstart
            this.end = nend
            this.referenceAllele = nref
            this.variantAllele = nvar
        }
    }

    private fun longestCommonPrefix(str1: String?, str2: String?): String {
        if (str1 == null || str2 == null) {
            return ""
        }
        for (prefixLen in 0 until str1.length) {
            val c = str1[prefixLen]
            if (prefixLen >= str2.length || str2[prefixLen] != c) {
                return str2.substring(0, prefixLen)
            }
        }
        return str1
    }
}