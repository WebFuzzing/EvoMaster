package org.evomaster.core.utils

import org.evomaster.core.problem.util.ParserDtoUtil.getJsonNodeFromText
import org.evomaster.core.problem.util.ParserDtoUtil.parseJsonNodeAsGene
import org.evomaster.core.problem.util.ParserDtoUtil.setGeneBasedOnString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParseDtoUtilTest {

    @Test
    fun testGrch37Response(){
        val response =  """
            [{"strand":-1,"seq_region_name":"1","id":"AGT:c.803T>C","assembly_name":"GRCh37","transcript_consequences":[{"transcript_id":"ENST00000366667","polyphen_score":0,"amino_acids":"M/T","gene_symbol":"AGT","sift_score":1,"cdna_end":1018,"gene_id":"ENSG00000135744","strand":-1,"polyphen_prediction":"benign","cds_start":803,"protein_end":268,"protein_start":268,"cdna_start":1018,"hgnc_id":333,"consequence_terms":["missense_variant"],"codons":"aTg/aCg","sift_prediction":"tolerated","cds_end":803,"impact":"MODERATE","gene_symbol_source":"HGNC","variant_allele":"C","biotype":"protein_coding"},{"gene_symbol":"RP11-99J16__A.2","transcript_id":"ENST00000412344","variant_allele":"C","biotype":"antisense","gene_symbol_source":"Clone_based_vega_gene","distance":650,"consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000244137","strand":-1,"impact":"MODIFIER"}],"end":230845794,"allele_string":"T/C","colocated_variants":[{"start":230845794,"seq_region_name":"1","strand":1,"phenotype_or_disease":1,"id":"CM920010","allele_string":"HGMD_MUTATION","end":230845794},{"end":230845794,"allele_string":"COSMIC_MUTATION","id":"COSV64184214","phenotype_or_disease":1,"strand":1,"seq_region_name":"1","var_synonyms":{"COSMIC":["COSM425562"]},"somatic":1,"start":230845794},{"start":230845794,"pubmed":[25741868,27274104,19131662,19263529,19330901,19559392,21515823,26819062,29972410,30571812,21146954,21894447,27584680,30409984,21467728,23036011,21127830,23021345,23497168,26824906,21681796,27380726,22099458,18513389,18603647,22817530,20577119,18248681,23205182,27616475,21058046,26621708,26933222,28770234,26627480,18953568,21304999,25474356,16059745,18069999,18279468,18698212,21056700,21533139,23333443,23681449,25512783,26172140,26335431,28666769,21540342,21919968,31343553,23133444,30387422,22100073,27454254,20486282,25157616,20981351,21573014,21444836,20047954,20570668,26966016,28690685,31396257,28605058,23438931,21306748,27068935,21261619,19108684,18637188,24722536,26283679,29627490,24622918,27940662,21438754,24452035,25683681,31511791,31858452,20811292,25723521,30295198,31090375,20592051,30455538,30621627,27480094,26588355,23132613,22858200,22569109,31560448,27348238,26509357,28903744,29057680,29520984,31048445,27910864,27342049,23354977,1394429,7649545,7883995,8348146,8513325,8518804,9259580,9421481,983133],"clin_sig":["benign","risk_factor"],"var_synonyms":{"ClinVar":["RCV000019693","RCV000019692","RCV000019691","VCV000018068","RCV000835695","RCV000405686","RCV000242838"],"UniProt":["VAR_007096"],"PharmGKB":["PA166153539"],"OMIM":[106150.0001]},"seq_region_name":"1","strand":1,"minor_allele":"A","phenotype_or_disease":1,"minor_allele_freq":0.2949,"clin_sig_allele":"G:risk_factor;G:benign","allele_string":"A/G","end":230845794,"id":"rs699","frequencies":{"C":{"gnomade_amr":0.7196,"gnomade_oth":0.5002,"gnomade_asj":0.44,"sas":0.636,"amr":0.6354,"afr":0.9032,"eas":0.8532,"gnomade_afr":0.8451,"gnomade_nfe":0.4197,"gnomade_sas":0.6202,"eur":0.4115,"gnomade_fin":0.441,"af":0.7051,"gnomade_eas":0.8388,"gnomade":0.5481}}}],"input":"AGT:c.803T>C","most_severe_consequence":"missense_variant","start":230845794},{"input":"9:g.22125503G>C","most_severe_consequence":"downstream_gene_variant","start":22125503,"colocated_variants":[{"allele_string":"G/A/C","end":22125503,"frequencies":{"C":{"afr":0.2133,"amr":0.4553,"sas":0.4908,"eur":0.4722,"eas":0.5367,"af":0.4181}},"id":"rs1333049","minor_allele":"C","clin_sig_allele":"C:risk_factor","phenotype_or_disease":1,"minor_allele_freq":0.4181,"clin_sig":["risk_factor"],"strand":1,"var_synonyms":{"ClinVar":["RCV001003460","VCV000812642"],"PharmGKB":["PA166157726"]},"seq_region_name":"9","start":22125503,"pubmed":[19343170,20386740,21860704,28209224,28686695,18362232,18675980,19214202,19475673,20231156,20858905,21152093,21698238,24906238,19474294,26677855,27015805,29765957,18852197,23729007,26958643,26999117,18469204,18533027,20605023,21372283,23468967,24932356,26950853,27424552,19463184,21971053,24676469,27096864,27507036,19559344,19578366,23963167,30558699,29972410,30571812,19171343,24246088,18987759,19207022,19819472,19926059,21375403,21385355,21705410,24607648,24777168,25105296,27317124,27721851,28138111,30072947,30387168,30594667,30814313,23870195,26252781,26982883,19888323,30065929,31543200,23454037,26483964,17634449,18224312,18264662,18599798,18652946,18654002,18704761,18780302,18925945,18957718,18979498,19135198,19164808,19173706,19319159,19329499,19501493,19548844,19664850,19709660,19750184,19885677,19924713,19955471,19956433,19956784,20017983,20031540,20031580,20031606,20098575,20159871,20175863,20230275,20335276,20395598,20400779,20427016,20435227,20502693,20549515,20649639,20670758,20923989,20981302,2114955]}],"assembly_name":"GRCh37","transcript_consequences":[{"distance":4407,"consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000240498","strand":1,"impact":"MODIFIER","gene_symbol_source":"HGNC","biotype":"antisense","variant_allele":"C","transcript_id":"ENST00000422420","hgnc_id":34341,"gene_symbol":"CDKN2B-AS1"},{"gene_symbol":"CDKN2B-AS1","hgnc_id":34341,"transcript_id":"ENST00000428597","biotype":"antisense","variant_allele":"C","gene_symbol_source":"HGNC","impact":"MODIFIER","distance":4409,"consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000240498","strand":1},{"consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000240498","distance":4932,"strand":1,"impact":"MODIFIER","gene_symbol_source":"HGNC","variant_allele":"C","biotype":"antisense","transcript_id":"ENST00000577551","hgnc_id":34341,"gene_symbol":"CDKN2B-AS1"},{"strand":1,"gene_id":"ENSG00000240498","consequence_terms":["downstream_gene_variant"],"distance":4858,"impact":"MODIFIER","gene_symbol_source":"HGNC","biotype":"antisense","variant_allele":"C","transcript_id":"ENST00000580576","hgnc_id":34341,"gene_symbol":"CDKN2B-AS1"},{"hgnc_id":34341,"gene_symbol":"CDKN2B-AS1","transcript_id":"ENST00000581051","gene_symbol_source":"HGNC","variant_allele":"C","biotype":"antisense","strand":1,"consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000240498","distance":4932,"impact":"MODIFIER"},{"hgnc_id":34341,"gene_symbol":"CDKN2B-AS1","transcript_id":"ENST00000582072","gene_symbol_source":"HGNC","variant_allele":"C","biotype":"antisense","impact":"MODIFIER","consequence_terms":["downstream_gene_variant"],"distance":4932,"gene_id":"ENSG00000240498","strand":1},{"gene_symbol":"CDKN2B-AS1","hgnc_id":34341,"transcript_id":"ENST00000584020","biotype":"antisense","variant_allele":"C","gene_symbol_source":"HGNC","impact":"MODIFIER","consequence_terms":["downstream_gene_variant"],"distance":4932,"gene_id":"ENSG00000240498","strand":1},{"hgnc_id":34341,"gene_symbol":"CDKN2B-AS1","transcript_id":"ENST00000584637","gene_symbol_source":"HGNC","biotype":"antisense","variant_allele":"C","distance":4932,"gene_id":"ENSG00000240498","consequence_terms":["downstream_gene_variant"],"strand":1,"impact":"MODIFIER"},{"transcript_id":"ENST00000584816","gene_symbol":"CDKN2B-AS1","hgnc_id":34341,"consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000240498","strand":1,"distance":4932,"impact":"MODIFIER","biotype":"antisense","variant_allele":"C","gene_symbol_source":"HGNC"},{"hgnc_id":34341,"gene_symbol":"CDKN2B-AS1","transcript_id":"ENST00000585267","gene_symbol_source":"HGNC","variant_allele":"C","biotype":"antisense","impact":"MODIFIER","consequence_terms":["downstream_gene_variant"],"gene_id":"ENSG00000240498","strand":1,"distance":4960}],"end":22125503,"allele_string":"G/C","strand":1,"regulatory_feature_consequences":[{"biotype":"promoter_flanking_region","variant_allele":"C","impact":"MODIFIER","regulatory_feature_id":"ENSR00002081590","consequence_terms":["regulatory_region_variant"]}],"seq_region_name":"9","id":"9:g.22125503G>C"}]
        """.trimIndent()

        checkConsistent(response)
    }

    @Test
    fun testNumberArrayResponseInOneFieldMap(){
        val response = """
            [{
              "OMIM" : [ 106150.0001 ]
            }]
        """.trimIndent()
        checkConsistent(response)
    }

    @Test
    fun testNumberArrayResponse(){
        val response = """
            [{
                "var_synonyms" : {
                  "ClinVar" : [ "RCV000019693", "RCV000019692", "RCV000019691", "VCV000018068", "RCV000835695", "RCV000405686", "RCV000242838" ],
                  "UniProt" : [ "VAR_007096" ],
                  "PharmGKB" : [ "PA166153539" ],
                  "OMIM" : [ 106150.0001 ]
                }
            }]
        """.trimIndent()
        checkConsistent(response)
    }

    private fun checkConsistent(response: String){
        val jsonNode = getJsonNodeFromText(response)
        assertNotNull(jsonNode)
        val parsedGene = parseJsonNodeAsGene("response", jsonNode!!)
        setGeneBasedOnString(parsedGene, response)

        val rawString = parsedGene.getValueAsRawString()
        val sameJsonNode = getJsonNodeFromText(rawString)
        assertEquals(sameJsonNode!!.size(),jsonNode.size())
        assertEquals(sameJsonNode.toPrettyString(), jsonNode.toPrettyString())
    }
}