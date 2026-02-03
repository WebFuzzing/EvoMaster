package org.evomaster.core.problem.rest

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.parser.OpenAPIParser
import org.evomaster.client.java.instrumentation.shared.ClassToSchemaUtils.OPENAPI_REF_PATH
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.ResourceCluster
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.TaintedMapGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.format.DateTimeFormatter

class RestActionBuilderV3Test{

    @BeforeEach
    fun reset(){
        RestActionBuilderV3.cleanCache()
    }


    @Test
    fun testDtoIssueWithWronglyHandledField(){

        val name = "org.cbioportal.genome_nexus.model.VariantAnnotation"

        val schema = """
            "org.cbioportal.genome_nexus.model.VariantAnnotation":{
               "org.cbioportal.genome_nexus.model.VariantAnnotation":{
                  "type":"object", 
                  "properties": {
                      "variant":{"type":"string"},
                       "annotationJSON":{"type":"string"},
                       "variantId":{"type":"string"},
                       "assemblyName":{"type":"string"},
                       "seqRegionName":{"type":"string"},
                       "start":{"type":"integer", "format":"int32"},
                       "end":{"type":"integer", "format":"int32"},
                       "alleleString":{"type":"string"},
                       "strand":{"type":"integer", "format":"int32"},
                       "mostSevereConsequence":{"type":"string"},
                       "colocatedVariants":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.ColocatedVariant"}},
                       "intergenicConsequences":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.IntergenicConsequences"}},
                       "transcriptConsequences":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.TranscriptConsequence"}},
                       "successfullyAnnotated":{"type":"boolean"},
                       "mutationAssessorAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.MutationAssessorAnnotation"},
                       "nucleotideContextAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.NucleotideContextAnnotation"},
                       "myVariantInfoAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.MyVariantInfoAnnotation"},
                       "hotspotAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.HotspotAnnotation"},
                       "ptmAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.PtmAnnotation"},
                       "oncokbAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.OncokbAnnotation"},
                       "clinvarAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.ClinvarAnnotation"},
                       "annotationSummary":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.VariantAnnotationSummary"},
                       "signalAnnotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.SignalAnnotation"},
                       "originalVariantQuery":{"type":"string"},
                       "dynamicProps":{"type":"object", "additionalProperties":{"${'$'}ref":"#/components/schemas/java.lang.Object"}}},
                  "required": ["variant","annotationJSON","variantId","assemblyName","seqRegionName","start","end","alleleString","strand","mostSevereConsequence","colocatedVariants","intergenicConsequences","transcriptConsequences","successfullyAnnotated","mutationAssessorAnnotation","nucleotideContextAnnotation","myVariantInfoAnnotation","hotspotAnnotation","ptmAnnotation","oncokbAnnotation","clinvarAnnotation","annotationSummary","signalAnnotation","originalVariantQuery","dynamicProps"]},
               "org.cbioportal.genome_nexus.model.ColocatedVariant":{"type":"object", "properties": {"gnomad_nfe_maf":{"type":"string"},"gnomad_nfe_allele":{"type":"string"},"gnomad_afr_maf":{"type":"string"},"gnomad_afr_allele":{"type":"string"},"gnomad_eas_maf":{"type":"string"},"gnomad_eas_allele":{"type":"string"},"dbSnpId":{"type":"string"}}, "required": ["gnomad_nfe_maf","gnomad_nfe_allele","gnomad_afr_maf","gnomad_afr_allele","gnomad_eas_maf","gnomad_eas_allele","dbSnpId"]},"org.cbioportal.genome_nexus.model.IntergenicConsequences":{"type":"object", "properties": {"impact":{"type":"string"},"variantAllele":{"type":"string"},"consequenceTerms":{"type":"array", "items":{"type":"string"}}}, "required": ["impact","variantAllele","consequenceTerms"]},"org.cbioportal.genome_nexus.model.TranscriptConsequence":{"type":"object", "properties": {"transcriptId":{"type":"string"},"hgvsp":{"type":"string"},"hgvsc":{"type":"string"},"hgvsg":{"type":"string"},"variantAllele":{"type":"string"},"codons":{"type":"string"},"proteinId":{"type":"string"},"proteinStart":{"type":"integer", "format":"int32"},"proteinEnd":{"type":"integer", "format":"int32"},"geneSymbol":{"type":"string"},"geneId":{"type":"string"},"aminoAcids":{"type":"string"},"hgncId":{"type":"string"},"canonical":{"type":"string"},"polyphenScore":{"type":"number", "format":"double"},"polyphenPrediction":{"type":"string"},"siftScore":{"type":"number", "format":"double"},"siftPrediction":{"type":"string"},"exon":{"type":"string"},"uniprotId":{"type":"string"},"refseqTranscriptIds":{"type":"array", "items":{"type":"string"}},"consequenceTerms":{"type":"array", "items":{"type":"string"}},"dynamicProps":{"type":"object", "additionalProperties":{"${'$'}ref":"#/components/schemas/java.lang.Object"}}}, "required": ["transcriptId","hgvsp","hgvsc","hgvsg","variantAllele","codons","proteinId","proteinStart","proteinEnd","geneSymbol","geneId","aminoAcids","hgncId","canonical","polyphenScore","polyphenPrediction","siftScore","siftPrediction","exon","uniprotId","refseqTranscriptIds","consequenceTerms","dynamicProps"]},"java.lang.Object":{"type":"object", "properties": {}, "required": []},"org.cbioportal.genome_nexus.model.MutationAssessorAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.MutationAssessor"}}, "required": ["license","annotation"]},"org.cbioportal.genome_nexus.model.MutationAssessor":{"type":"object", "properties": {"input":{"type":"string"},"hgvs":{"type":"string"},"hugoSymbol":{"type":"string"},"referenceGenomeVariant":{"type":"string"},"referenceGenomeVariantType":{"type":"string"},"functionalImpact":{"type":"string"},"functionalImpactScore":{"type":"number", "format":"double"},"msaLink":{"type":"string"},"pdbLink":{"type":"string"},"variantConservationScore":{"type":"number", "format":"double"},"variantSpecificityScore":{"type":"number", "format":"double"},"mappingIssue":{"type":"string"},"variant":{"type":"string"},"uniprotId":{"type":"string"},"refseqId":{"type":"string"},"msaGaps":{"type":"number", "format":"double"},"msaHeight":{"type":"integer", "format":"int32"},"codonStartPosition":{"type":"string"},"uniprotPosition":{"type":"integer", "format":"int32"},"uniprotResidue":{"type":"string"},"refseqPosition":{"type":"integer", "format":"int32"},"refseqResidue":{"type":"string"},"cosmicCount":{"type":"integer", "format":"int32"},"snpCount":{"type":"integer", "format":"int32"}}, "required": ["input","hgvs","hugoSymbol","referenceGenomeVariant","referenceGenomeVariantType","functionalImpact","functionalImpactScore","msaLink","pdbLink","variantConservationScore","variantSpecificityScore","mappingIssue","variant","uniprotId","refseqId","msaGaps","msaHeight","codonStartPosition","uniprotPosition","uniprotResidue","refseqPosition","refseqResidue","cosmicCount","snpCount"]},"org.cbioportal.genome_nexus.model.NucleotideContextAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.NucleotideContext"}}, "required": ["license","annotation"]},"org.cbioportal.genome_nexus.model.NucleotideContext":{"type":"object", "properties": {"query":{"type":"string"},"molecule":{"type":"string"},"id":{"type":"string"},"seq":{"type":"string"},"hgvs":{"type":"string"}}, "required": ["query","molecule","id","seq","hgvs"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.MyVariantInfoAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.MyVariantInfo"}}, "required": ["license","annotation"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.MyVariantInfo":{"type":"object", "properties": {"variant":{"type":"string"},"query":{"type":"string"},"hgvs":{"type":"string"},"version":{"type":"integer", "format":"int32"},"snpeff":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Snpeff"},"vcf":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Vcf"},"dbsnp":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Dbsnp"},"cosmic":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Cosmic"},"clinvar":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.ClinVar"},"mutdb":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Mutdb"},"gnomad_exome":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Gnomad"},"gnomad_genome":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Gnomad"}}, "required": ["variant","query","hgvs","version","snpeff","vcf","dbsnp","cosmic","clinvar","mutdb","gnomad_exome","gnomad_genome"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Snpeff":{"type":"object", "properties": {"_license":{"type":"string"}}, "required": ["_license"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Vcf":{"type":"object", "properties": {"alt":{"type":"string"},"position":{"type":"string"},"ref":{"type":"string"}}, "required": ["alt","position","ref"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Dbsnp":{"type":"object", "properties": {"_license":{"type":"string"},"allele_origin":{"type":"string"},"alleles":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Alleles"}},"alt":{"type":"string"},"chrom":{"type":"string"},"_class":{"type":"string"},"dbsnp_build":{"type":"integer", "format":"int32"},"flags":{"type":"array", "items":{"type":"string"}},"hg19":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Hg19"},"ref":{"type":"string"},"rsid":{"type":"string"},"validated":{"type":"boolean"},"var_subtype":{"type":"string"},"vartype":{"type":"string"}}, "required": ["_license","allele_origin","alleles","alt","chrom","_class","dbsnp_build","flags","hg19","ref","rsid","validated","var_subtype","vartype"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Alleles":{"type":"object", "properties": {"allele":{"type":"string"}}, "required": ["allele"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Hg19":{"type":"object", "properties": {"start":{"type":"integer", "format":"int32"},"end":{"type":"integer", "format":"int32"}}, "required": ["start","end"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Cosmic":{"type":"object", "properties": {"_license":{"type":"string"},"alt":{"type":"string"},"chrom":{"type":"string"},"cosmic_id":{"type":"string"},"hg19":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Hg19"},"mut_freq":{"type":"number", "format":"double"},"mut_nt":{"type":"string"},"ref":{"type":"string"},"tumor_site":{"type":"string"}}, "required": ["_license","alt","chrom","cosmic_id","hg19","mut_freq","mut_nt","ref","tumor_site"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.ClinVar":{"type":"object", "properties": {"_license":{"type":"string"},"allele_id":{"type":"integer", "format":"int32"},"alt":{"type":"string"},"chrom":{"type":"string"},"cytogenic":{"type":"string"},"gene":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Gene"},"hg19":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Hg19"},"hg38":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Hg38"},"hgvs":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Hgvs"},"rcv":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Rcv"}},"variant_id":{"type":"integer", "format":"int32"}}, "required": ["_license","allele_id","alt","chrom","cytogenic","gene","hg19","hg38","hgvs","rcv","variant_id"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Gene":{"type":"object", "properties": {"gene_id":{"type":"string"},"symbol":{"type":"string"}}, "required": ["gene_id","symbol"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Hg38":{"type":"object", "properties": {"start":{"type":"string"},"end":{"type":"string"}}, "required": ["start","end"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Hgvs":{"type":"object", "properties": {"coding":{"type":"array", "items":{"type":"string"}},"genomic":{"type":"array", "items":{"type":"string"}},"protein":{"type":"array", "items":{"type":"string"}}}, "required": ["coding","genomic","protein"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Rcv":{"type":"object", "properties": {"accession":{"type":"string"},"clinical_significance":{"type":"string"},"origin":{"type":"string"},"preferred_name":{"type":"string"}}, "required": ["accession","clinical_significance","origin","preferred_name"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Mutdb":{"type":"object", "properties": {"rsid":{"type":"string"},"ref":{"type":"string"},"alt":{"type":"string"},"uniprot_id":{"type":"string"},"mutpred_score":{"type":"number", "format":"double"},"cosmic_id":{"type":"string"},"chrom":{"type":"string"},"hg19":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Hg19"}}, "required": ["rsid","ref","alt","uniprot_id","mutpred_score","cosmic_id","chrom","hg19"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Gnomad":{"type":"object", "properties": {"ac":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.AlleleCount"},"an":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.AlleleNumber"},"af":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.AlleleFrequency"},"hom":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.my_variant_info_model.Homozygotes"}}, "required": ["ac","an","af","hom"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.AlleleCount":{"type":"object", "properties": {"ac":{"type":"integer", "format":"int32"},"ac_afr":{"type":"integer", "format":"int32"},"ac_amr":{"type":"integer", "format":"int32"},"ac_asj":{"type":"integer", "format":"int32"},"ac_eas":{"type":"integer", "format":"int32"},"ac_fin":{"type":"integer", "format":"int32"},"ac_nfe":{"type":"integer", "format":"int32"},"ac_oth":{"type":"integer", "format":"int32"},"ac_sas":{"type":"integer", "format":"int32"}}, "required": ["ac","ac_afr","ac_amr","ac_asj","ac_eas","ac_fin","ac_nfe","ac_oth","ac_sas"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.AlleleNumber":{"type":"object", "properties": {"an":{"type":"integer", "format":"int32"},"an_afr":{"type":"integer", "format":"int32"},"an_amr":{"type":"integer", "format":"int32"},"an_asj":{"type":"integer", "format":"int32"},"an_eas":{"type":"integer", "format":"int32"},"an_fin":{"type":"integer", "format":"int32"},"an_nfe":{"type":"integer", "format":"int32"},"an_oth":{"type":"integer", "format":"int32"},"an_sas":{"type":"integer", "format":"int32"}}, "required": ["an","an_afr","an_amr","an_asj","an_eas","an_fin","an_nfe","an_oth","an_sas"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.AlleleFrequency":{"type":"object", "properties": {"af":{"type":"number", "format":"double"},"af_afr":{"type":"number", "format":"double"},"af_amr":{"type":"number", "format":"double"},"af_asj":{"type":"number", "format":"double"},"af_eas":{"type":"number", "format":"double"},"af_fin":{"type":"number", "format":"double"},"af_nfe":{"type":"number", "format":"double"},"af_oth":{"type":"number", "format":"double"},"af_sas":{"type":"number", "format":"double"}}, "required": ["af","af_afr","af_amr","af_asj","af_eas","af_fin","af_nfe","af_oth","af_sas"]},"org.cbioportal.genome_nexus.model.my_variant_info_model.Homozygotes":{"type":"object", "properties": {"hom":{"type":"integer", "format":"int32"},"hom_afr":{"type":"integer", "format":"int32"},"hom_amr":{"type":"integer", "format":"int32"},"hom_asj":{"type":"integer", "format":"int32"},"hom_eas":{"type":"integer", "format":"int32"},"hom_fin":{"type":"integer", "format":"int32"},"hom_nfe":{"type":"integer", "format":"int32"},"hom_oth":{"type":"integer", "format":"int32"},"hom_sas":{"type":"integer", "format":"int32"}}, "required": ["hom","hom_afr","hom_amr","hom_asj","hom_eas","hom_fin","hom_nfe","hom_oth","hom_sas"]},"org.cbioportal.genome_nexus.model.HotspotAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"type":"array", "items":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.Hotspot"}}}}, "required": ["license","annotation"]},"org.cbioportal.genome_nexus.model.Hotspot":{"type":"object", "properties": {"id":{"type":"string"},"hugo_symbol":{"type":"string"},"transcript_id":{"type":"string"},"residue":{"type":"string"},"tumor_count":{"type":"integer", "format":"int32"},"type":{"type":"string"},"missense_count":{"type":"integer", "format":"int32"},"trunc_count":{"type":"integer", "format":"int32"},"inframe_count":{"type":"integer", "format":"int32"},"splice_count":{"type":"integer", "format":"int32"}}, "required": ["id","hugo_symbol","transcript_id","residue","tumor_count","type","missense_count","trunc_count","inframe_count","splice_count"]},"org.cbioportal.genome_nexus.model.PtmAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"type":"array", "items":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.PostTranslationalModification"}}}}, "required": ["license","annotation"]},"org.cbioportal.genome_nexus.model.PostTranslationalModification":{"type":"object", "properties": {"uniprot_entry":{"type":"string"},"uniprot_accession":{"type":"string"},"ensembl_transcript_ids":{"type":"array", "items":{"type":"string"}},"position":{"type":"integer", "format":"int32"},"type":{"type":"string"},"pubmed_ids":{"type":"array", "items":{"type":"string"}},"sequence":{"type":"string"}}, "required": ["uniprot_entry","uniprot_accession","ensembl_transcript_ids","position","type","pubmed_ids","sequence"]},"org.cbioportal.genome_nexus.model.OncokbAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"${'$'}ref":"#/components/schemas/org.oncokb.client.IndicatorQueryResp"}}, "required": ["license","annotation"]},"org.oncokb.client.IndicatorQueryResp":{"type":"object", "properties": {"alleleExist":{"type":"boolean"},"dataVersion":{"type":"string"},"diagnosticImplications":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.oncokb.client.Implication"}},"diagnosticSummary":{"type":"string"},"geneExist":{"type":"boolean"},"geneSummary":{"type":"string"},"highestDiagnosticImplicationLevel":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]},"highestPrognosticImplicationLevel":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]},"highestResistanceLevel":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]},"highestSensitiveLevel":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]},"hotspot":{"type":"boolean"},"lastUpdate":{"type":"string"},"mutationEffect":{"${'$'}ref":"#/components/schemas/org.oncokb.client.MutationEffectResp"},"oncogenic":{"type":"string"},"otherSignificantResistanceLevels":{"type":"array", "items":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]}},"otherSignificantSensitiveLevels":{"type":"array", "items":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]}},"prognosticImplications":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.oncokb.client.Implication"}},"prognosticSummary":{"type":"string"},"query":{"${'$'}ref":"#/components/schemas/org.oncokb.client.Query"},"treatments":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.oncokb.client.IndicatorQueryTreatment"}},"tumorTypeSummary":{"type":"string"},"variantExist":{"type":"boolean"},"variantSummary":{"type":"string"},"vus":{"type":"boolean"}}, "required": ["alleleExist","dataVersion","diagnosticImplications","diagnosticSummary","geneExist","geneSummary","highestDiagnosticImplicationLevel","highestPrognosticImplicationLevel","highestResistanceLevel","highestSensitiveLevel","hotspot","lastUpdate","mutationEffect","oncogenic","otherSignificantResistanceLevels","otherSignificantSensitiveLevels","prognosticImplications","prognosticSummary","query","treatments","tumorTypeSummary","variantExist","variantSummary","vus"]},"org.oncokb.client.Implication":{"type":"object", "properties": {"alterations":{"type":"array", "items":{"type":"string"}},"description":{"type":"string"},"levelOfEvidence":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]},"tumorType":{"${'$'}ref":"#/components/schemas/org.oncokb.client.TumorType"}}, "required": ["alterations","description","levelOfEvidence","tumorType"]},"org.oncokb.client.TumorType":{"type":"object", "properties": {"children":{"type":"object", "additionalProperties":{"${'$'}ref":"#/components/schemas/org.oncokb.client.TumorType"}},"code":{"type":"string"},"color":{"type":"string"},"id":{"type":"integer", "format":"int32"},"level":{"type":"integer", "format":"int32"},"mainType":{"${'$'}ref":"#/components/schemas/org.oncokb.client.MainType"},"name":{"type":"string"},"parent":{"type":"string"},"tissue":{"type":"string"},"tumorForm":{"type":"string", "enum":["SOLID","LIQUID"]}}, "required": ["children","code","color","id","level","mainType","name","parent","tissue","tumorForm"]},"org.oncokb.client.MainType":{"type":"object", "properties": {"id":{"type":"integer", "format":"int32"},"name":{"type":"string"},"tumorForm":{"type":"string", "enum":["SOLID","LIQUID"]}}, "required": ["id","name","tumorForm"]},"org.oncokb.client.MutationEffectResp":{"type":"object", "properties": {"citations":{"${'$'}ref":"#/components/schemas/org.oncokb.client.Citations"},"description":{"type":"string"},"knownEffect":{"type":"string"}}, "required": ["citations","description","knownEffect"]},"org.oncokb.client.Citations":{"type":"object", "properties": {"abstracts":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.oncokb.client.ArticleAbstract"}},"pmids":{"type":"array", "items":{"type":"string"}}}, "required": ["abstracts","pmids"]},"org.oncokb.client.ArticleAbstract":{"type":"object", "properties": {"abstract":{"type":"string"},"link":{"type":"string"}}, "required": ["abstract","link"]},"org.oncokb.client.Query":{"type":"object", "properties": {"alteration":{"type":"string"},"alterationType":{"type":"string"},"consequence":{"type":"string"},"entrezGeneId":{"type":"integer", "format":"int32"},"hgvs":{"type":"string"},"hugoSymbol":{"type":"string"},"id":{"type":"string"},"proteinEnd":{"type":"integer", "format":"int32"},"proteinStart":{"type":"integer", "format":"int32"},"svType":{"type":"string", "enum":["DELETION","TRANSLOCATION","DUPLICATION","INSERTION","INVERSION","FUSION","UNKNOWN"]},"tumorType":{"type":"string"},"type":{"type":"string"}}, "required": ["alteration","alterationType","consequence","entrezGeneId","hgvs","hugoSymbol","id","proteinEnd","proteinStart","svType","tumorType","type"]},"org.oncokb.client.IndicatorQueryTreatment":{"type":"object", "properties": {"abstracts":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.oncokb.client.ArticleAbstract"}},"alterations":{"type":"array", "items":{"type":"string"}},"approvedIndications":{"type":"array", "items":{"type":"string"}},"description":{"type":"string"},"drugs":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.oncokb.client.Drug"}},"fdaApproved":{"type":"boolean"},"level":{"type":"string", "enum":["LEVEL_0","LEVEL_1","LEVEL_2","LEVEL_2A","LEVEL_2B","LEVEL_3A","LEVEL_3B","LEVEL_4","LEVEL_R1","LEVEL_R2","LEVEL_R3","LEVEL_PX1","LEVEL_PX2","LEVEL_PX3","LEVEL_DX1","LEVEL_DX2","LEVEL_DX3","NO"]},"levelAssociatedCancerType":{"${'$'}ref":"#/components/schemas/org.oncokb.client.TumorType"},"pmids":{"type":"array", "items":{"type":"string"}}}, "required": ["abstracts","alterations","approvedIndications","description","drugs","fdaApproved","level","levelAssociatedCancerType","pmids"]},"org.oncokb.client.Drug":{"type":"object", "properties": {"drugName":{"type":"string"},"ncitCode":{"type":"string"},"synonyms":{"type":"array", "items":{"type":"string"}},"uuid":{"type":"string"}}, "required": ["drugName","ncitCode","synonyms","uuid"]},"org.cbioportal.genome_nexus.model.ClinvarAnnotation":{"type":"object", "properties": {"annotation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.Clinvar"}}, "required": ["annotation"]},"org.cbioportal.genome_nexus.model.Clinvar":{"type":"object", "properties": {"chromosome":{"type":"string"},"start_position":{"type":"integer", "format":"int32"},"end_position":{"type":"integer", "format":"int32"},"reference_allele":{"type":"string"},"alternate_allele":{"type":"string"},"clinvar_id":{"type":"integer", "format":"int32"},"clnsig":{"type":"string"},"clnsigconf":{"type":"string"}}, "required": ["chromosome","start_position","end_position","reference_allele","alternate_allele","clinvar_id","clnsig","clnsigconf"]},"org.cbioportal.genome_nexus.model.VariantAnnotationSummary":{"type":"object", "properties": {"variant":{"type":"string"},"genomicLocation":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.GenomicLocation"},"strandSign":{"type":"string"},"variantType":{"type":"string"},"assemblyName":{"type":"string"},"canonicalTranscriptId":{"type":"string"},"transcriptConsequences":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.TranscriptConsequenceSummary"}},"transcriptConsequenceSummaries":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.TranscriptConsequenceSummary"}},"transcriptConsequenceSummary":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.TranscriptConsequenceSummary"}}, "required": ["variant","genomicLocation","strandSign","variantType","assemblyName","canonicalTranscriptId","transcriptConsequences","transcriptConsequenceSummaries","transcriptConsequenceSummary"]},"org.cbioportal.genome_nexus.model.GenomicLocation":{"type":"object", "properties": {"chromosome":{"type":"string"},"start":{"type":"integer", "format":"int32"},"end":{"type":"integer", "format":"int32"},"referenceAllele":{"type":"string"},"variantAllele":{"type":"string"}}, "required": ["chromosome","start","end","referenceAllele","variantAllele"]},"org.cbioportal.genome_nexus.model.TranscriptConsequenceSummary":{"type":"object", "properties": {"transcriptId":{"type":"string"},"codonChange":{"type":"string"},"aminoAcids":{"type":"string"},"aminoAcidRef":{"type":"string"},"aminoAcidAlt":{"type":"string"},"entrezGeneId":{"type":"string"},"consequenceTerms":{"type":"string"},"hugoGeneSymbol":{"type":"string"},"hgvspShort":{"type":"string"},"hgvsp":{"type":"string"},"hgvsc":{"type":"string"},"proteinPosition":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.IntegerRange"},"refSeq":{"type":"string"},"variantClassification":{"type":"string"},"exon":{"type":"string"},"polyphenScore":{"type":"number", "format":"double"},"polyphenPrediction":{"type":"string"},"siftScore":{"type":"number", "format":"double"},"siftPrediction":{"type":"string"},"uniprotId":{"type":"string"}}, "required": ["transcriptId","codonChange","aminoAcids","aminoAcidRef","aminoAcidAlt","entrezGeneId","consequenceTerms","hugoGeneSymbol","hgvspShort","hgvsp","hgvsc","proteinPosition","refSeq","variantClassification","exon","polyphenScore","polyphenPrediction","siftScore","siftPrediction","uniprotId"]},"org.cbioportal.genome_nexus.model.IntegerRange":{"type":"object", "properties": {"start":{"type":"integer", "format":"int32"},"end":{"type":"integer", "format":"int32"}}, "required": ["start","end"]},"org.cbioportal.genome_nexus.model.SignalAnnotation":{"type":"object", "properties": {"license":{"type":"string"},"annotation":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.SignalMutation"}}}, "required": ["license","annotation"]},"org.cbioportal.genome_nexus.model.SignalMutation":{"type":"object", "properties": {"hugo_gene_symbol":{"type":"string"},"chromosome":{"type":"string"},"start_position":{"type":"integer", "format":"int64"},"end_position":{"type":"integer", "format":"int64"},"reference_allele":{"type":"string"},"variant_allele":{"type":"string"},"mutation_status":{"type":"string"},"pathogenic":{"type":"string"},"penetrance":{"type":"string"},"n_germline_homozygous":{"type":"integer", "format":"int32"},"counts_by_tumor_type":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.CountByTumorType"}},"biallelic_counts_by_tumor_type":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.CountByTumorType"}},"qc_pass_counts_by_tumor_type":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.CountByTumorType"}},"general_population_stats":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.GeneralPopulationStats"},"msk_expert_review":{"type":"boolean"},"stats_by_tumor_type":{"type":"array", "items":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.StatsByTumorType"}}}, "required": ["hugo_gene_symbol","chromosome","start_position","end_position","reference_allele","variant_allele","mutation_status","pathogenic","penetrance","n_germline_homozygous","counts_by_tumor_type","biallelic_counts_by_tumor_type","qc_pass_counts_by_tumor_type","general_population_stats","msk_expert_review","stats_by_tumor_type"]},"org.cbioportal.genome_nexus.model.CountByTumorType":{"type":"object", "properties": {"tumor_type":{"type":"string"},"tumor_type_count":{"type":"integer", "format":"int32"},"variant_count":{"type":"integer", "format":"int32"}}, "required": ["tumor_type","tumor_type_count","variant_count"]},"org.cbioportal.genome_nexus.model.GeneralPopulationStats":{"type":"object", "properties": {"counts":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.SignalPopulationStats"},"frequencies":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.SignalPopulationStats"}}, "required": ["counts","frequencies"]},"org.cbioportal.genome_nexus.model.SignalPopulationStats":{"type":"object", "properties": {"impact":{"type":"number", "format":"double"},"eur":{"type":"number", "format":"double"},"afr":{"type":"number", "format":"double"},"asn":{"type":"number", "format":"double"},"asj":{"type":"number", "format":"double"},"oth":{"type":"number", "format":"double"}}, "required": ["impact","eur","afr","asn","asj","oth"]},"org.cbioportal.genome_nexus.model.StatsByTumorType":{"type":"object", "properties": {"tumor_type":{"type":"string"},"n_cancer_type_count":{"type":"integer", "format":"int32"},"f_cancer_type_count":{"type":"number", "format":"double"},"f_biallelic":{"type":"number", "format":"double"},"age_at_dx":{"type":"integer", "format":"int32"},"tmb":{"type":"number", "format":"double"},"msi_score":{"type":"number", "format":"double"},"n_with_sig":{"type":"integer", "format":"int32"},"hrd_score":{"${'$'}ref":"#/components/schemas/org.cbioportal.genome_nexus.model.HrdScore"},"n_germline_homozygous":{"type":"integer", "format":"int32"}}, "required": ["tumor_type","n_cancer_type_count","f_cancer_type_count","f_biallelic","age_at_dx","tmb","msi_score","n_with_sig","hrd_score","n_germline_homozygous"]},"org.cbioportal.genome_nexus.model.HrdScore":{"type":"object", "properties": {"lst":{"type":"number", "format":"double"},"ntelomeric_ai":{"type":"number", "format":"double"},"fraction_loh":{"type":"number", "format":"double"}}, "required": ["lst","ntelomeric_ai","fraction_loh"]}}
        """.trimIndent()

        //should not crash
        val gene = RestActionBuilderV3.createGeneForDTO(name, schema, RestActionBuilderV3.Options(invalidData = false))
    }


    @Test
    fun testDereferencing(){
        val map = loadAndAssertActions("/swagger/artificial/dereferencing.yaml",1, true)
        val a = map.values.first() as RestCallAction

        val topGenes = a.seeTopGenes()
        assertEquals(5, topGenes.size)
        assertTrue(topGenes.any { it is IntegerGene})
        assertTrue(topGenes.any { it.getWrappedGene(BooleanGene::class.java) != null })
        assertTrue(topGenes.any {
            it is ObjectGene
            && it.fields.size == 1
            && it.fields[0].getWrappedGene(DoubleGene::class.java) != null
        })
        assertTrue(topGenes.any { it is EnumGene<*> })

        assertEquals(1, a.produces.size)
        assertTrue(a.produces.contains("application/xml"))
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testTraceV2(enableConstraintHandling : Boolean){
        /*
            Swagger Parser for V2 seems buggy, as ignoring TRACE.
            See: io.swagger.parser.util.SwaggerDeserializer#path
         */
        loadAndAssertActions("/swagger/artificial/trace_v2.json", 0, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testTraceV3(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/artificial/trace_v3.json", 1, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testEnumYml(enableConstraintHandling : Boolean){

        val map = loadAndAssertActions("/swagger/artificial/openapi-enum.yml", 2, enableConstraintHandling = enableConstraintHandling)
        val get = map["GET:/api/enum"]!!
        val post = map["POST:/api/enum"]!!

        val getEnums = get.seeTopGenes().flatMap { it.flatView() }.filterIsInstance<EnumGene<*>>()
        assertEquals(2, getEnums.size)
        val postEnums = post.seeTopGenes().flatMap { it.flatView() }.filterIsInstance<EnumGene<*>>()
        assertEquals(1, postEnums.size)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testArrayAnyOfAndOneOfSchema(enableConstraintHandling : Boolean){
        val schema = OpenAPIParser().readLocation("swagger/apisguru-v3/adyen_checkoutservice41.yaml", null, null).openAPI
        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(schema, actions, enableConstraintHandling = enableConstraintHandling)

        val postPaymentMethodsBalance = actions["POST:/v41/payments"]
        assertTrue(postPaymentMethodsBalance is RestCallAction)
        (postPaymentMethodsBalance as RestCallAction).apply {
            assertEquals(2, parameters.size)
            val requestBody = parameters.filterIsInstance<BodyParam>()
            assertEquals(1, requestBody.size)
            requestBody.first().genes.first { it.name == "body" }.apply {
                val valueGene = ParamUtil.getValueGene(this)
                assertTrue(valueGene is ObjectGene)

                // for anyOf
                (valueGene as ObjectGene).fields.find { it.name == "additionalData" }.apply {
                    assertNotNull(this)
                    assertTrue(this is OptionalGene)
                    (this as OptionalGene).gene.apply {
                        if (enableConstraintHandling){
                            assertTrue(this is ChoiceGene<*>)
                            /*
                                15 references of anyOf plus one which combine all
                             */
                            assertEquals(15 + 1,  (this as ChoiceGene<*>).lengthOfChildren())
                        }else{
                            assertTrue(this is FixedMapGene<*, *>)
                        }
                    }
                }


                // for oneOf
                valueGene.fields.find { it.name == "paymentMethod" }.apply {
                    assertNotNull(this)
                    // paymentMethod is required
                    assertFalse(this is OptionalGene)
                    if (enableConstraintHandling){
                        assertTrue(this is ChoiceGene<*>)
                        /*
                            39 references of oneOf
                         */
                        assertEquals(39,  (this as ChoiceGene<*>).lengthOfChildren())
                    }else{
                        assertTrue(this is TaintedMapGene)
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testAllOfSchema(enableConstraintHandling : Boolean){
        val name = "FileShare"
        val dtoSchema = """
            "$name": {
                "$name": {
                  "allOf": [
                    {
                      "description": "Base resource object.",
                      "properties": {
                        "id": {
                          "description": "URI of the resource.",
                          "readOnly": true,
                          "type": "string"
                        },
                        "location": {
                          "description": "The region where the resource is located.",
                          "type": "string"
                        },
                        "name": {
                          "description": "Name of the resource.",
                          "readOnly": true,
                          "type": "string"
                        },
                        "tags": {
                          "additionalProperties": {
                            "type": "string"
                          },
                          "description": "List of key-value pairs.",
                          "type": "object"
                        },
                        "type": {
                          "description": "Type of resource.",
                          "readOnly": true,
                          "type": "string"
                        }
                      },
                      "type": "object",
                      "x-ms-azure-resource": true
                    }
                  ],
                  "description": "Object that contains properties of the file share resource.",
                  "properties": {
                    "properties": {
                      "${'$'}ref": "#/definitions/FileShareModel",
                      "description": "Properties of a file share resource.",
                      "x-ms-client-flatten": true
                    }
                  },
                  "type": "object"
                },
                "FileShareModel": {
                  "description": "Properties of a file share resource.",
                  "properties": {
                    "associatedVolume": {
                      "description": "Associated volume ID.",
                      "type": "string"
                    },
                    "uncPath": {
                      "description": "The UNCPath for the fileshare.",
                      "type": "string"
                    }
                  },
                  "type": "object"
                }
            }
        """.trimIndent()

        val gene = RestActionBuilderV3.createGeneForDTO(name, dtoSchema, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
        assertEquals(name, gene.name)
        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            if (enableConstraintHandling)
                assertEquals(6, fields.size)
            else
                assertEquals(1, fields.size)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testPropertiesWithAdditionalProperties(enableConstraintHandling : Boolean){
        //example from APIs_guru/azure.com/cognitiveservices-LUIS-Runtime/2.0
        val name = "EntityModel"
        val dtoSchema = """
            "$name": {
              "additionalProperties": {
                "description": "List of additional properties. For example, score and resolution values for pre-built LUIS entities.",
                "type": "object"
              },
              "description": "An entity extracted from the utterance.",
              "properties": {
                "endIndex": {
                  "description": "The position of the last character of the matched entity within the utterance.",
                  "type": "integer"
                },
                "entity": {
                  "description": "Name of the entity, as defined in LUIS.",
                  "type": "string"
                },
                "startIndex": {
                  "description": "The position of the first character of the matched entity within the utterance.",
                  "type": "integer"
                },
                "type": {
                  "description": "Type of the entity, as defined in LUIS.",
                  "type": "string"
                }
              },
              "required": [
                "entity",
                "type",
                "startIndex",
                "endIndex"
              ],
              "type": "object"
            }
        """.trimIndent()

        val gene = RestActionBuilderV3.createGeneForDTO(name, dtoSchema, null, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
        assertEquals(name, gene.name)

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(4, fixedFields.size)
            //assertTrue(template!!.second is FixedMapGene<*, *>)
            assertTrue(template!!.second is TaintedMapGene, "Template is of type ${template!!.second::class.simpleName}")
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testArrayWithAdditionalProperties(enableConstraintHandling : Boolean){
        //example from APIs_guru/googleapis.com/apigateway/v1
        val name = "ApigatewayStatus"
        val dtoSchema = """
          "$name": {
            "description": "The `Status` type defines a logical error model that is suitable for different programming environments, including REST APIs and RPC APIs. It is used by [gRPC](https://github.com/grpc). Each `Status` message contains three pieces of data: error code, error message, and error details. You can find out more about this error model and how to work with it in the [API Design Guide](https://cloud.google.com/apis/design/errors).",
            "properties": {
              "code": {
                "description": "The status code, which should be an enum value of google.rpc.Code.",
                "format": "int32",
                "type": "integer"
              },
              "details": {
                "description": "A list of messages that carry the error details. There is a common set of message types for APIs to use.",
                "items": {
                  "additionalProperties": {
                    "description": "Properties of the object. Contains field @type with type URL."
                  },
                  "type": "object"
                },
                "type": "array"
              },
              "message": {
                "description": "A developer-facing error message, which should be in English. Any user-facing error message should be localized and sent in the google.rpc.Status.details field, or localized by the client.",
                "type": "string"
              }
            },
            "type": "object"
          }
        """.trimIndent()

        val gene = RestActionBuilderV3.createGeneForDTO(name, dtoSchema, null, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling)) as ObjectGene

        assertEquals(name, gene.name)
        assertEquals(3, gene.fields.size)

        val details = gene.fields.find { it.name == "details" }
        assertNotNull(details)
        val detailsGene = ParamUtil.getValueGene(details!!)
        assertTrue(detailsGene is ArrayGene<*>)
        (detailsGene as ArrayGene<*>).apply {
            assertTrue(template is FixedMapGene<*, *>)
            (template as FixedMapGene<*, *>).apply {
                assertTrue(template.first is StringGene)
                assertTrue(template.second is StringGene)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testAdditionalPropertiesWithObjectType(enableConstraintHandling : Boolean){
        // example from APIs_guru/azure.com/containerinstance-containerInstance/2018-10-01
        val name = "ContainerGroupIdentity"
        val dtoSchema = """
            "$name": {
              "description": "Identity for the container group.",
              "properties": {
                "principalId": {
                  "description": "The principal id of the container group identity. This property will only be provided for a system assigned identity.",
                  "readOnly": true,
                  "type": "string"
                },
                "tenantId": {
                  "description": "The tenant id associated with the container group. This property will only be provided for a system assigned identity.",
                  "readOnly": true,
                  "type": "string"
                },
                "type": {
                  "description": "The type of identity used for the container group. The type 'SystemAssigned, UserAssigned' includes both an implicitly created identity and a set of user assigned identities. The type 'None' will remove any identities from the container group.",
                  "enum": [
                    "SystemAssigned",
                    "UserAssigned",
                    "SystemAssigned, UserAssigned",
                    "None"
                  ],
                  "type": "string",
                  "x-ms-enum": {
                    "modelAsString": false,
                    "name": "ResourceIdentityType"
                  }
                },
                "userAssignedIdentities": {
                  "additionalProperties": {
                    "properties": {
                      "clientId": {
                        "description": "The client id of user assigned identity.",
                        "readOnly": true,
                        "type": "string"
                      },
                      "principalId": {
                        "description": "The principal id of user assigned identity.",
                        "readOnly": true,
                        "type": "string"
                      }
                    },
                    "type": "object"
                  },
                  "description": "The list of user identities associated with the container group. The user identity dictionary key references will be ARM resource ids in the form: '/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.ManagedIdentity/userAssignedIdentities/{identityName}'.",
                  "type": "object"
                }
              }
            }
        """.trimIndent()

        val gene = RestActionBuilderV3.createGeneForDTO(name, dtoSchema, null, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling)) as ObjectGene

        assertEquals(name, gene.name)
        assertEquals(4, gene.fields.size)

        val userAssignedIdentities = gene.fields.find { it.name == "userAssignedIdentities" }
        assertNotNull(userAssignedIdentities)
        val userAssignedIdentitiesGene = ParamUtil.getValueGene(userAssignedIdentities!!)
        assertTrue(userAssignedIdentitiesGene is FixedMapGene<*, *>)
        (userAssignedIdentitiesGene as FixedMapGene<*, *>).apply {
            assertTrue(template.first is StringGene)
            assertTrue(template.second is ObjectGene)
            (template.second as ObjectGene).apply {
                assertEquals(2, fields.size)
                assertTrue(fields.any { f-> f.name == "principalId" })
                assertTrue(fields.any { f-> f.name == "clientId" })
            }
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testDtoEnum(enableConstraintHandling : Boolean){
        val name = "org.evomaster.client.java.instrumentation.object.dtos.DtoEnum"

        val dtoSchema = """
            "$name":{"type":"object", "properties": {"foo":{"type":"string"},"bar":{"type":"string", "enum":["ONE","TWO","THREE"]}}}
        """.trimIndent()


        val gene = RestActionBuilderV3.createGeneForDTO(name, dtoSchema, name,
            RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling, invalidData = false)) as ObjectGene
        assertEquals(name, gene.name)
        assertEquals(2, gene.fields.size)

        gene.fields.find { ParamUtil.getValueGene(it) is StringGene }.apply {
            assertNotNull(this)
            assertNotNull(ParamUtil.getValueGene(this!!) is StringGene)
            (ParamUtil.getValueGene(this) as StringGene).apply {
                assertEquals("foo", this.name)
            }
        }

        gene.fields.find { ParamUtil.getValueGene(it) is EnumGene<*> }.apply {
            assertNotNull(this)
            assertNotNull(ParamUtil.getValueGene(this!!) is EnumGene<*>)
            (ParamUtil.getValueGene(this) as EnumGene<String>).apply {
                assertEquals("bar", this.name)
                assertEquals(3, values.size)
                listOf("ONE","TWO","THREE").forEach {  s ->
                    assertTrue(values.contains(s))
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testParseDto(enableConstraintHandling : Boolean){


        val name = "com.FooBar"
        val foo = "foo"
        val bar = "bar"

        val dtoSchema = """
            "$name": {
                 "type": "object",
                 "properties": {
                        "$foo": { 
                            "type": "string"
                        },
                        "$bar": {
                            "type": "integer"
                        }
                 },
                 "required": [
                    "$foo"
                 ]
            }     
        """.trimIndent()

        val gene = RestActionBuilderV3.createGeneForDTO(name, dtoSchema, name, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling)) as ObjectGene
        assertEquals(name, gene.name)
        assertEquals(2, gene.fields.size)

        val str = gene.fields.find { it is StringGene } as StringGene
        assertEquals(foo, str.name)

        val nr = gene.fields.find { it is OptionalGene } as OptionalGene
        assertEquals(bar, nr.name)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testParseDtos(enableConstraintHandling : Boolean){

        val nameFoo = "evo.Foo"
        val nameBar = "evo.Bar"

        val dtoSchemaFoo = """
            "$nameFoo": {
                 "type": "object",
                 "properties": {
                        "bar": { 
                            "${'$'}ref": "${OPENAPI_REF_PATH}evo.Bar"
                        }
                 },
                 "required": [
                    "bar"
                 ]
            }     
        """.trimIndent()

        val dtoSchemaBar = """
            "$nameBar": {
                 "type": "object",
                 "properties": {
                        "foo": { 
                            "${'$'}ref": "${OPENAPI_REF_PATH}evo.Foo"
                        }
                 },
                 "required": [
                    "foo"
                 ]
            }     
        """.trimIndent()

        val objGenes = RestActionBuilderV3.createGenesForDTOs(listOf(nameFoo, nameBar), listOf(dtoSchemaFoo, dtoSchemaBar), listOf(nameFoo, nameBar), RestActionBuilderV3.Options(enableConstraintHandling = enableConstraintHandling))
        assertEquals(2, objGenes.size)

        assertEquals(nameFoo, objGenes[0].name)
        assertTrue(objGenes[0] is ObjectGene)
        assertEquals(1, (objGenes[0] as ObjectGene).fields.size)

        val barField = ((objGenes[0] as ObjectGene).fields.find { it is ObjectGene } as ObjectGene)
        assertEquals("bar", barField.name)
        val cycleBar = (barField.fields[0] as ObjectGene).fields[0]
        assertTrue(cycleBar is CycleObjectGene)


        assertEquals(nameBar, objGenes[1].name)
        assertTrue(objGenes[1] is ObjectGene)
        assertEquals(1, (objGenes[1] as ObjectGene).fields.size)

        val fooField = ((objGenes[1] as ObjectGene).fields.find { it is ObjectGene } as ObjectGene)
        assertEquals("foo", fooField.name)
        val cycleFoo = (fooField.fields[0] as ObjectGene).fields[0]
        assertTrue(cycleFoo is CycleObjectGene)

    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testParseMapDto(enableConstraintHandling : Boolean){
        val mapDto = "org.evomaster.client.java.instrumentation.object.dtos.MapDto"

        val allSchema = """
            "$mapDto":{
               "org.evomaster.client.java.instrumentation.object.dtos.MapDto":{
                  "type":"object",
                  "properties":{
                     "mapDtoArray":{
                        "type":"object",
                        "additionalProperties":{
                           "${'$'}ref":"#/components/schemas/org.evomaster.client.java.instrumentation.object.dtos.DtoArray"
                        }
                     },
                     "mapInteger":{
                        "type":"object",
                        "additionalProperties":{
                           "type":"integer",
                           "format":"int32"
                        }
                     }
                  }
               },
               "org.evomaster.client.java.instrumentation.object.dtos.DtoArray":{
                  "type":"object",
                  "properties":{
                     "array":{
                        "type":"array",
                        "items":{
                           "type":"string"
                        }
                     },
                     "set":{
                        "type":"array",
                        "items":{
                           "type":"integer",
                           "format":"int32"
                        }
                     },
                     "set_raw":{
                        "type":"array",
                        "items":{
                           "type":"string"
                        }
                     },
                     "list":{
                        "type":"array",
                        "items":{
                           "type":"boolean"
                        }
                     },
                     "list_raw":{
                        "type":"array",
                        "items":{
                           "type":"string"
                        }
                     }
                  }
               }
            }
        """.trimIndent()

        val mapGene = RestActionBuilderV3.createGeneForDTO(mapDto, allSchema, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
        assertTrue(mapGene is ObjectGene)
        (mapGene as ObjectGene).apply {
            assertEquals(2, fields.size)
            val mapArrayField = ParamUtil.getValueGene(fields.find { it.name == "mapDtoArray" }!!)
            assertTrue(mapArrayField is FixedMapGene<*, *>)
            (mapArrayField as FixedMapGene<*,*>).apply {
                assertTrue(template.first is StringGene)
                assertTrue(template.second is ObjectGene)

                (template.second as ObjectGene).apply {
                    assertEquals(5, fields.size)
                    assertEquals("org.evomaster.client.java.instrumentation.object.dtos.DtoArray", refType)
                }
            }

            val mapIntField = ParamUtil.getValueGene(fields.find { it.name == "mapInteger" }!!)
            assertTrue(mapIntField is FixedMapGene<*,*>)
            (mapIntField as FixedMapGene<*,*>).apply {
                assertTrue(template.first is StringGene)
                assertTrue(template.second is IntegerGene)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testGHOrgnization(enableConstraintHandling : Boolean){
        val classToExtract = "org.kohsuke.github.GHOrganization"
        val schema = """"$classToExtract":{
               "org.kohsuke.github.GHOrganization":{
                  "type":"object",
                  "properties":{
                     "root":{
                        "${'$'}ref":"#/components/schemas/org.kohsuke.github.GitHub"
                     },
                     "login":{
                        "type":"string"
                     },
                     "avatar_url":{
                        "type":"string"
                     },
                     "gravatar_id":{
                        "type":"string"
                     },
                     "location":{
                        "type":"string"
                     },
                     "blog":{
                        "type":"string"
                     },
                     "email":{
                        "type":"string"
                     },
                     "name":{
                        "type":"string"
                     },
                     "company":{
                        "type":"string"
                     },
                     "html_url":{
                        "type":"string"
                     },
                     "followers":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "following":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "public_repos":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "public_gists":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "url":{
                        "type":"string"
                     },
                     "id":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "created_at":{
                        "type":"string"
                     },
                     "updated_at":{
                        "type":"string"
                     }
                  }
               },
               "org.kohsuke.github.GitHub":{
                  "type":"object",
                  "properties":{
                     "login":{
                        "type":"string"
                     },
                     "encodedAuthorization":{
                        "type":"string"
                     },
                     "users":{
                        "type":"object",
                        "additionalProperties":{
                           "${'$'}ref":"#/components/schemas/org.kohsuke.github.GHUser"
                        }
                     },
                     "orgs":{
                        "type":"object",
                        "additionalProperties":{
                           "${'$'}ref":"#/components/schemas/org.kohsuke.github.GHOrganization"
                        }
                     },
                     "apiUrl":{
                        "type":"string"
                     },
                     "rateLimitHandler":{
                        "${'$'}ref":"#/components/schemas/org.kohsuke.github.RateLimitHandler"
                     },
                     "abuseLimitHandler":{
                        "${'$'}ref":"#/components/schemas/org.kohsuke.github.AbuseLimitHandler"
                     },
                     "connector":{
                        "${'$'}ref":"#/components/schemas/org.kohsuke.github.HttpConnector"
                     }
                  }
               },
               "org.kohsuke.github.GHUser":{
                  "type":"object",
                  "properties":{
                     "root":{
                        "${'$'}ref":"#/components/schemas/org.kohsuke.github.GitHub"
                     },
                     "login":{
                        "type":"string"
                     },
                     "avatar_url":{
                        "type":"string"
                     },
                     "gravatar_id":{
                        "type":"string"
                     },
                     "location":{
                        "type":"string"
                     },
                     "blog":{
                        "type":"string"
                     },
                     "email":{
                        "type":"string"
                     },
                     "name":{
                        "type":"string"
                     },
                     "company":{
                        "type":"string"
                     },
                     "html_url":{
                        "type":"string"
                     },
                     "followers":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "following":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "public_repos":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "public_gists":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "url":{
                        "type":"string"
                     },
                     "id":{
                        "type":"integer",
                        "format":"int32"
                     },
                     "created_at":{
                        "type":"string"
                     },
                     "updated_at":{
                        "type":"string"
                     }
                  }
               },
               "org.kohsuke.github.RateLimitHandler":{
                  "type":"object",
                  "properties":{
                     
                  }
               },
               "org.kohsuke.github.AbuseLimitHandler":{
                  "type":"object",
                  "properties":{
                     
                  }
               },
               "org.kohsuke.github.HttpConnector":{
                  "type":"object",
                  "properties":{
                     
                  }
               }
            }
        """
        val ghGene = RestActionBuilderV3.createGeneForDTO(classToExtract, schema, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
    }

    //---------------------------------

    private fun loadAndAssertActions(resourcePath: String, expectedNumberOfActions: Int, enableConstraintHandling: Boolean)
            : MutableMap<String, Action> {
        return loadAndAssertActions(resourcePath, expectedNumberOfActions, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
    }

    private fun loadAndAssertActions(resourcePath: String, expectedNumberOfActions: Int, options: RestActionBuilderV3.Options)
            : MutableMap<String, Action> {

        val holder = RestSchema(OpenApiAccess.getOpenAPIFromResource(resourcePath))

        val actions: MutableMap<String, Action> = mutableMapOf()

        val errors = RestActionBuilderV3.addActionsFromSwagger(holder, actions, options=options)
        errors.forEach {
            println(it)
        }

        assertEquals(expectedNumberOfActions, actions.size)

        //should not crash
        //RestActionBuilderV3.getModelsFromSwagger(holder.main.schemaParsed, mutableMapOf(), options = options)

        return actions
    }

    private fun checkNumOfRootGene(actionCluster: Map<String, Action>,
                                   skipActions: List<String>,
                                   expectedNumberOfActions: Int,
                                   expectedNumOfRootGene: Int,
                                   expectedNumOfIG0: Int,
                                   expectedNumOfIGM: Int,
                                   expectedNumOfObjOfIGM: Int){


        val cluster = actionCluster.filterNot { skipActions.contains(it.key.split(":")[1]) }
        assertEquals(expectedNumberOfActions, cluster.size)

        var numOfRG = 0
        var numOfIG0 = 0
        var numOfIGM = 0
        var numOfObjOfIGM = 0

        cluster.values.forEach { a->
            numOfRG += a.seeTopGenes().size
            numOfIG0 += a.seeTopGenes().count { g-> g.getViewOfChildren().isEmpty() }
            numOfIGM += a.seeTopGenes().count { g-> g.getViewOfChildren().isNotEmpty() }
            numOfObjOfIGM += a.seeTopGenes().count { g-> ParamUtil.getValueGene(g) is ObjectGene }
        }
        assertEquals(expectedNumOfRootGene, numOfRG)
        assertEquals(expectedNumOfIG0, numOfIG0)
        assertEquals(expectedNumOfIGM, numOfIGM)
        assertEquals(expectedNumOfObjOfIGM, numOfObjOfIGM)
    }

    private fun checkNumResource(actionCluster: Map<String, Action>, skipActions: List<String>, numOfResource: Int, numOfIndResource: Int){
        val manipulated  = actionCluster.filterNot { skipActions.contains(it.key.split(":")[1]) }

        val cluster = ResourceCluster()
        val config = EMConfig()
        config.doesApplyNameMatching = true
        cluster.initResourceCluster(manipulated, config = config)

        assertEquals(numOfResource, cluster.getCluster().size)
        assertEquals(numOfIndResource, cluster.getCluster().count { it.value.isIndependent() })
    }

    // ----------- V3 --------------

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testadyen_checkoutservice41(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/apisguru-v3/adyen_checkoutservice41.yaml", 18, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testNexmo(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/apisguru-v3/nexmo.json", 5, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testBcgnews(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/bcgnws.json", 14, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testBclaws(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/bclaws.json", 7, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testBng2latlong(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/bng2latlong.json", 1, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testChecker(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/checker.json", 1, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testDisposable(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/disposable.json", 1, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testFraudDetection(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/fraud-detection.json", 2, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testGeolocation(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/geolocation.json", 1, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testIp2proxy(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/ip2proxy.com.json", 1, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testApisGuruNews(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/news.json", 27, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testOpen511(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/open511.json", 4, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSmsVerification(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/sms-verification.json", 2, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testValidation(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/apisguru-v3/validation.json", 1, enableConstraintHandling = enableConstraintHandling)
    }



    // ----------- V2 --------------

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testGitLab(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/others/gitlab.json", 358, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testCyclotron(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/cyclotron.json", 50, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),50, 100, 16, 84, 11)
        checkNumResource(map, listOf(), 40, 18)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testPetStore(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/others/petstore.json", 20, enableConstraintHandling = enableConstraintHandling)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testMultiParamPath(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/artificial/multi_param_path.json", 1, enableConstraintHandling = enableConstraintHandling)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testNcs(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/ncs.json", 6, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),6, 14, 0, 14, 0)
        checkNumResource(map, listOf(), 6, 6)

    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testScs(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/scs.json", 11, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),11, 26, 0, 26, 0)
        checkNumResource(map, listOf(), 11, 11)

    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testGestaoHospital(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/gestaohospital.json", 20, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),20, 51, 14, 37, 6)
        checkNumResource(map, listOf(), 13, 0)

    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testDisease(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/disease_sh_api.json", 34, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),34, 57, 0, 57, 0)
        checkNumResource(map, listOf(), 34, 34)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testRealWorld(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/realworld_app.json", 19, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),19, 37, 6, 31, 6)
        checkNumResource(map, listOf(), 11, 2)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSpaceX(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/spacex_api.json", 94, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),94, 131, 29, 102, 29)
        checkNumResource(map, listOf(), 52, 5)
    }



    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testNews(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/news.json", 7, enableConstraintHandling = enableConstraintHandling)

        val create = map["POST:/news"] as RestCallAction
        assertEquals(3, create.seeTopGenes().size)
        val bodyNews = create.seeTopGenes().find { it.name == "body" }
        assertNotNull(bodyNews)
        assertNotNull(bodyNews is OptionalGene)
        assertNotNull((bodyNews as OptionalGene).gene is ObjectGene)
        assertNotNull((bodyNews.gene as ObjectGene).refType)
        assertEquals("NewsDto", (bodyNews.gene as ObjectGene).refType)

        checkNumOfRootGene(map, listOf(),7, 15, 3, 12, 2)
        checkNumResource(map, listOf(), 4, 1)

    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testCatWatch(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/catwatch.json", 23, enableConstraintHandling = enableConstraintHandling)

        val postScoring = map["POST:/config/scoring.project"] as RestCallAction
        assertEquals(4, postScoring.seeTopGenes().size)
        val bodyPostScoring = postScoring.seeTopGenes().find { it.name == "body" }
        assertNotNull(bodyPostScoring)
        assertTrue(bodyPostScoring is OptionalGene)
        assertTrue((bodyPostScoring as OptionalGene).gene is StringGene)

        val skipInEM = listOf("/fetch", "/health", "/health.json", "/error")
        checkNumOfRootGene(map,skipInEM ,13, 38, 4, 34, 1)
        checkNumResource(map, skipInEM, 13, 11)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testProxyPrint(enableConstraintHandling : Boolean){

        //TODO check  Map<String, String> in additionalProperties

        val map = loadAndAssertActions("/swagger/sut/proxyprint.json", 115, enableConstraintHandling = enableConstraintHandling)

        val balance = map["GET:/consumer/balance"] as RestCallAction
        //Principal should not appear, because anyway it is a GET
        assertTrue(balance.parameters.none { it is BodyParam })


        val update = map["PUT:/consumer/info/update"] as RestCallAction
        //   Type is JSON, but no body info besides wrong Principal
        assertTrue(update.parameters.none { it is BodyParam })


        val register = map["POST:/consumer/register"] as RestCallAction
        // Same for WebRequest
        assertTrue(register.parameters.none { it is BodyParam })

        val skipInEM = listOf(
            "/heapdump", "/heapdump.json",
            "/autoconfig", "/autoconfig.json",
            "/beans", "/beans.json",
            "/configprops", "/configprops.json",
            "/dump", "/dump.json",
            "/env", "/env.json", "/env/{name}",
            "/error",
            "/health", "/health.json",
            "/info", "/info.json",
            "/mappings", "/mappings.json",
            "/metrics", "/metrics.json", "/metrics/{name}",
            "/trace", "/trace.json"
        )
        checkNumOfRootGene(map, skipInEM, 74, 106,24, 82, 14)

        checkNumResource(map, skipInEM, 56, 20)

    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testCreateActions(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/artificial/positive_integer_swagger.json", 2, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSchemaWithErrorEndpoint(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/artificial/positive_integer_swagger_errors.json", 1, enableConstraintHandling = enableConstraintHandling)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testOCVN(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/ocvn_1oc.json", 192, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),192, 2852, 0, 2852, 0)
        checkNumResource(map, listOf(), 96, 0)

    }

    @Disabled("This is a bug in Swagger Core, reported at https://github.com/swagger-api/swagger-core/issues/2100")
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testFeaturesServicesNull(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/sut/features_service_null.json", 18, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testFeaturesServices(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/features_service.json", 18, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),18, 41, 4, 37, 4)
        checkNumResource(map, listOf(), 11, 0)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testScoutApi(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/scout-api.json", 49, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),49, 146, 19, 127, 19)
        checkNumResource(map, listOf(), 21, 2)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testLanguageTool(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/languagetool.json", 2, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),2, 3, 1, 2, 1)
        checkNumResource(map, listOf(), 2, 1)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testRestCountries(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/restcountries.yaml", 22, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),22, 34, 2, 32, 0)
        checkNumResource(map, listOf(), 22, 22)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testCwaVerification(enableConstraintHandling : Boolean){
        val map = loadAndAssertActions("/swagger/sut/cwa_verification.json", 5, enableConstraintHandling = enableConstraintHandling)
        checkNumOfRootGene(map, listOf(),5, 16, 4, 12, 5)
        checkNumResource(map, listOf(), 5, 0)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testK0(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/others/k0.json", 20, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testK1(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/others/k1.json", 53, enableConstraintHandling = enableConstraintHandling)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testBranches(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/artificial/branches.json", 3, enableConstraintHandling = enableConstraintHandling)
    }



    //TODO need to handle "multipart/form-data"
    @Disabled
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testSimpleForm(enableConstraintHandling : Boolean){
        val actions = loadAndAssertActions("/swagger/artificial/simpleform.json", 1, enableConstraintHandling = enableConstraintHandling)

        val a = actions.values.first() as RestCallAction

        assertEquals(HttpVerb.POST, a.verb)
        assertEquals(2, a.parameters.size)
        assertEquals(2, a.parameters.filter { p -> p is FormParam }.size)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testDuplicatedParamsInFeaturesServices(enableConstraintHandling : Boolean){
        val actions = loadAndAssertActions("/swagger/sut/features_service.json", 18, enableConstraintHandling = enableConstraintHandling)
        (actions["POST:/products/{productName}/configurations/{configurationName}/features/{featureName}"] as RestCallAction).apply {
            assertEquals(3, parameters.size)
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testApisGuru(enableConstraintHandling : Boolean){

        val actions = loadAndAssertActions("/swagger/apisguru-v2/apis.guru.json", 2, enableConstraintHandling = enableConstraintHandling)

        actions.values
                .filterIsInstance<RestCallAction>()
                .forEach {
                    assertEquals(2, it.produces.size)
                    assertTrue(it.produces.any{ p -> p.contains("application/json")})
                }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testGreenPeace(enableConstraintHandling : Boolean){
        loadAndAssertActions("/swagger/apisguru-v2/greenpeace.org.json", 6, enableConstraintHandling = enableConstraintHandling)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testRestApiExample(enableConstraintHandling : Boolean){
        val resourcePath = "/swagger/others/rest-api-example.json"
        val actions = loadAndAssertActions(resourcePath, 3, enableConstraintHandling = enableConstraintHandling)

        val get = actions["GET:/api/items"]
        assertNotNull(get)

        //No longer valid
//        val schema = OpenAPIParser().readLocation(resourcePath, null, null).openAPI
//        val map = mutableMapOf<String,ObjectGene>()
//        RestActionBuilderV3.getModelsFromSwagger(schema, map, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
//
//        assertEquals(3, map.size)
//        assertTrue(map.containsKey("Iterable«Item»"), "Not found key Iterable«Item». Current keys: ${map.keys.joinToString(", ")}.")
//        val x = map["Iterable«Item»"] as ObjectGene //this is due to bug in SpringFox that does not handle Iterable<T>
//        assertEquals(0, x.fields.size)
    }


    @ParameterizedTest
    @ValueSource(strings = ["/swagger/artificial/reference_type_v2.json","/swagger/artificial/reference_type_v3.json"])
    fun testReferenceType(path: String){
        val actions = loadAndAssertActions(path, 1, enableConstraintHandling = false)
        val bodyParam = actions.values.filterIsInstance<RestCallAction>().flatMap { it.parameters }.filterIsInstance<BodyParam>()
        assertEquals(1, bodyParam.size)
        assertTrue(bodyParam.first().gene is ObjectGene)
        (bodyParam.first().gene as ObjectGene).apply {
            assertNotNull(refType)
            assertEquals("Component", refType)
            val info = (fields.find { it.name == "info" } as? OptionalGene)
            assertNotNull(info)
            assertTrue((info as OptionalGene).gene is ObjectGene)
            (info.gene as ObjectGene).apply {
                assertNotNull(refType)
                assertEquals("Info", refType)
                val at = (fields.find { it.name =="at" } as? OptionalGene)
                assertNotNull(at)
                assertTrue((at as OptionalGene).gene is ArrayGene<*>)
                (at.gene as ArrayGene<*>).apply {
                    assertTrue(template is ObjectGene)
                    assertEquals("AT", (template as ObjectGene).refType)
                }
            }

        }
    }


    @Test
    fun testDefaultInt(){
        val path = "/swagger/artificial/defaultandexamples/default_int.yml"

        val without = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 0.0))
                        .values.first()
        assertEquals(1, without.seeTopGenes().size)
        val x = without.seeTopGenes().first().flatView()
        assertTrue(x.any { it is IntegerGene })
        assertTrue(x.none { it is ChoiceGene<*> })
        assertTrue(x.none {it is EnumGene<*>})

        val with = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 0.5))
                        .values.first()
        assertEquals(1, with.seeTopGenes().size)
        val y = with.seeTopGenes().first().flatView()
        assertTrue(y.any { it is IntegerGene })
        assertTrue(y.any { it is ChoiceGene<*> })
        assertTrue(y.any {it is EnumGene<*>})

        val certain = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 1.0))
            .values.first()
            .seeTopGenes().first()
        val output = certain.getValueAsRawString()
        assertEquals("42", output)
    }



    @Test
    fun testDefaultStringQuery(){
        val path = "/swagger/artificial/defaultandexamples/default_string_query.yml"

        val without = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 0.0))
            .values.first()
        assertEquals(1, without.seeTopGenes().size)
        val x = without.seeTopGenes().first().flatView()
        assertTrue(x.any { it is StringGene })
        assertTrue(x.none { it is ChoiceGene<*> })
        assertTrue(x.none {it is EnumGene<*>})


        val with = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 0.5))
            .values.first()
        assertEquals(1, with.seeTopGenes().size)
        val y = with.seeTopGenes().first().flatView()
        assertTrue(y.any { it is StringGene })
        assertTrue(y.any { it is ChoiceGene<*> })
        assertTrue(y.any {it is EnumGene<*>})


        val certain = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 1.0))
            .values.first()
            .seeTopGenes().first()
        val output = certain.getValueAsRawString()
        assertEquals("Foo", output)
    }

    @Test
    fun testDefaultStringPath(){
        val path = "/swagger/artificial/defaultandexamples/default_string_path.yml"

        val a = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 0.1))
            .values.first()

        val rand = Randomness()
        a.doInitialize(rand)

        var isFoo = false
        var isInvalid = false

        for(i in 0..1000){
            a.randomize(rand,false)
            val s = a.seeTopGenes().first().getValueAsRawString()
            if(s == "foo"){
                isFoo = true
            }
            if(s.isEmpty() || s.contains("/")){
                isInvalid = true
            }
            if(isFoo && isInvalid){
                break
            }
        }

        assertTrue(isFoo)
        assertFalse(isInvalid)
    }



    @ParameterizedTest
    @ValueSource(strings = ["/swagger/artificial/defaultandexamples/example_int_in.yml",
        "/swagger/artificial/defaultandexamples/example_int_out.yml"])
    fun testExampleInt(path: String){

        val without = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 0.0))
            .values.first()
        assertEquals(1, without.seeTopGenes().size)
        val x = without.seeTopGenes().first().flatView()
        assertTrue(x.any { it is IntegerGene })
        assertTrue(x.none { it is ChoiceGene<*> })
        assertTrue(x.none {it is EnumGene<*>})

        val with = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 0.5))
            .values.first()
        assertEquals(1, with.seeTopGenes().size)
        val y = with.seeTopGenes().first().flatView()
        assertTrue(y.any { it is IntegerGene })
        assertTrue(y.any { it is ChoiceGene<*> })
        assertTrue(y.any {it is EnumGene<*>})

        val certain = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 1.0))
            .values.first()
            .seeTopGenes().first()
        val output = certain.getValueAsRawString()
        assertEquals("42", output)
    }


    @ParameterizedTest
    @ValueSource(strings = [
        "/swagger/artificial/defaultandexamples/examples_object_single_in.yaml",
        "/swagger/artificial/defaultandexamples/examples_object_single_out.yaml",
        "/swagger/artificial/defaultandexamples/default_object_single.yaml"
    ])
    fun testExampleObjectSingle(path: String){
        val a = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 0.5, probUseDefault = 0.5))
            .values.first()

        val rand = Randomness()
        a.doInitialize(rand)

        var Bar42Pos = false
        var Bar42Neg = false

        data class ObjectSingleDto(
            var id: Int?,
            var name: String?,
            var extra: Int?
        ){
            constructor() : this(null,null,null)
        }
        val mapper = ObjectMapper()

        for(i in 0..1000){
            a.randomize(rand,false)
            val s = a.seeTopGenes().first().getValueAsRawString()

            val dto = mapper.readValue(s, ObjectSingleDto::class.java)

            if(dto.id == 42 && dto.name=="Bar" && dto.extra != null){
                if(dto.extra!! >= 0){
                    Bar42Pos = true
                } else {
                    Bar42Neg = true
                }
            }

            if(Bar42Pos && Bar42Neg){
                break
            }
        }

        assertTrue(Bar42Pos)
        assertTrue(Bar42Neg)

    }


    @Test
    fun testExampleObjectMulti(){
        val a = loadAndAssertActions("/swagger/artificial/defaultandexamples/examples_object_multi.yaml", 1, RestActionBuilderV3.Options(probUseExamples = 0.5, probUseDefault = 0.5))
            .values.first()

        val rand = Randomness()
        a.doInitialize(rand)

        var Bar42 = false
        var Foo123 = false

        data class ObjectSingleDto(
            var id: Int?,
            var name: String?,
            var extra: Int?
        ){
            constructor() : this(null,null,null)
        }
        val mapper = ObjectMapper()

        for(i in 0..1000){
            a.randomize(rand,false)
            val s = a.seeTopGenes().first().getValueAsRawString()

            val dto = mapper.readValue(s, ObjectSingleDto::class.java)

            if(dto.id == 42 && dto.name=="Bar"){
                Bar42 = true
            }
            if(dto.id == 123 && dto.name == "Foo" && dto.extra == 77){
                Foo123 = true
            }

            if(Bar42 && Foo123){
                break
            }
        }

        assertTrue(Bar42)
        assertTrue(Foo123)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/swagger/artificial/defaultandexamples/examples_string_in.yml",
        "/swagger/artificial/defaultandexamples/examples_string_out.yml"])
    fun testExamplesString(path: String){

        val a = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 0.5))
            .values.first()

        val rand = Randomness()
        a.doInitialize(rand)

        var isFoo = false
        var isBar = false
        var isHello = false

        for(i in 0..1000){
            a.randomize(rand,false)
            val s = a.seeTopGenes().first().getValueAsRawString()
            if(s == "Foo"){
                isFoo = true
            } else if(s == "Bar") {
                isBar = true
            } else if(s == "Hello"){
                isHello = true
            }
            if(isFoo && isBar && isHello){
                break
            }
        }

        assertTrue(isFoo)
        assertTrue(isBar)
        assertTrue(isHello)
    }

    @Test
    fun testExampleDefault(){
        val path = "/swagger/artificial/defaultandexamples/example_default.yml"

        val without = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 0.0, probUseDefault = 0.0))
            .values.first()
        assertEquals(1, without.seeTopGenes().size)
        val x = without.seeTopGenes().first().flatView()
        assertTrue(x.any { it is IntegerGene })
        assertTrue(x.none { it is ChoiceGene<*> })
        assertTrue(x.none {it is EnumGene<*>})

        val with = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 0.5, probUseDefault = 0.5))
            .values.first()
        assertEquals(1, with.seeTopGenes().size)
        val y = with.seeTopGenes().first().flatView()
        assertTrue(y.any { it is IntegerGene })
        assertTrue(y.any { it is ChoiceGene<*> })
        assertTrue(y.any {it is EnumGene<*>})

        val certainExample = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 1.0))
            .values.first()
            .seeTopGenes().first()
            .getValueAsRawString()
        assertEquals("42", certainExample)

        val certainDefault = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseDefault = 1.0))
            .values.first()
            .seeTopGenes().first()
            .getValueAsRawString()
        assertEquals("13", certainDefault)
    }


    @Test
    fun testExamplesAll() {

        val path = "/swagger/artificial/defaultandexamples/examples_string_all.yml"

        val a = loadAndAssertActions(path, 1, RestActionBuilderV3.Options(probUseExamples = 1.0))
            .values.first()

        val rand = Randomness()
        a.doInitialize(rand)

        val found = mutableSetOf<String>()

        for (i in 0..1000) {
            a.randomize(rand, false)
            val s = a.seeTopGenes().first().getValueAsRawString()
            found.add(s)
            if(found.size == 5){
                break
            }
        }

        assertEquals(5, found.size)
        assertTrue(found.contains("A"))
        assertTrue(found.contains("B"))
        assertTrue(found.contains("D"))
        assertTrue(found.contains("E"))
        assertTrue(found.contains("F"))
    }

    @Test
    fun testInt8AndInt16Formats() {

        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "short_primitive":{"type":"integer", "format":"int16"},
                    "short_wrapped":{"type":"integer", "format":"int16"},
                    "byte_primitive":{"type":"integer", "format":"int8"},
                    "byte_wrapped":{"type":"integer", "format":"int8"}
                },
                "required": [
                    "short_primitive",
                    "byte_primitive"
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(4, gene.fields.size)
            assertEquals("short_primitive", gene.fields[0].name)
            assertEquals("short_wrapped", gene.fields[1].name)
            assertEquals("byte_primitive", gene.fields[2].name)
            assertEquals("byte_wrapped", gene.fields[3].name)

            assertTrue(gene.fields[0] is IntegerGene)
            assertTrue(gene.fields[1] is OptionalGene)
            assertTrue(gene.fields[2] is IntegerGene)
            assertTrue(gene.fields[3] is OptionalGene)

            val shortPrimitiveGene = (gene.fields[0] as IntegerGene)
            assertEquals(Short.MIN_VALUE.toInt(), shortPrimitiveGene.min)
            assertEquals(Short.MAX_VALUE.toInt(), shortPrimitiveGene.max)

            val bytePrimitiveGene = (gene.fields[2] as IntegerGene)
            assertEquals(Byte.MIN_VALUE.toInt(), bytePrimitiveGene.min)
            assertEquals(Byte.MAX_VALUE.toInt(), bytePrimitiveGene.max)

            val shortWrappedGene = (gene.fields[1] as OptionalGene).gene
            assertTrue(shortWrappedGene is IntegerGene)
            (shortWrappedGene as IntegerGene).apply {
                assertEquals(Short.MIN_VALUE.toInt(), shortWrappedGene.min)
                assertEquals(Short.MAX_VALUE.toInt(), shortWrappedGene.max)
            }

            val byteWrappedGene = (gene.fields[3] as OptionalGene).gene
            assertTrue(byteWrappedGene is IntegerGene)
            (byteWrappedGene as IntegerGene).apply {
                assertEquals(Byte.MIN_VALUE.toInt(), byteWrappedGene.min)
                assertEquals(Byte.MAX_VALUE.toInt(), byteWrappedGene.max)
            }

        }
    }


    @Test
    fun testInt8AndInt16FormatsWithConstraints() {

        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "short_primitive":{
                        "type":"integer", 
                        "format":"int16",
                        "minimum": 0,
                        "maximum": 16
                    },
                    "byte_primitive":{
                        "type":"integer", 
                        "format":"int8",
                        "minimum": -1,
                        "maximum": 5
                    }
                },
                "required": [
                    "short_primitive",
                    "byte_primitive"
                    
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(2, gene.fields.size)
            assertEquals("short_primitive", gene.fields[0].name)
            assertEquals("byte_primitive", gene.fields[1].name)

            assertTrue(gene.fields[0] is IntegerGene)
            assertTrue(gene.fields[1] is IntegerGene)

            val shortPrimitiveGene = (gene.fields[0] as IntegerGene)
            assertEquals(0, shortPrimitiveGene.min)
            assertEquals(16, shortPrimitiveGene.max)

            val bytePrimitiveGene = (gene.fields[1] as IntegerGene)
            assertEquals(-1, bytePrimitiveGene.min)
            assertEquals(5, bytePrimitiveGene.max)


        }
    }


    @Test
    fun testDateField() {

        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "date_field":{
                        "type":"string", 
                        "format":"date"
                    }
                },
                "required": [
                    "date_field"
                    
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(1, gene.fields.size)
            assertEquals("date_field", gene.fields[0].name)
            assertTrue(gene.fields[0] is DateGene)
        }
    }

    @Test
    fun testDateTimeField() {

        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "date_time_field":{
                        "type":"string", 
                        "format":"date-time"
                    }
                },
                "required": [
                    "date_time_field"
                    
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(1, gene.fields.size)
            assertEquals("date_time_field", gene.fields[0].name)
            assertTrue(gene.fields[0] is DateTimeGene)
        }
    }

    @Test
    fun testLocalDateField() {

        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "local_date_field":{
                        "type":"string", 
                        "format":"local-date"
                    }
                },
                "required": [
                    "local_date_field"
                    
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(1, gene.fields.size)
            assertEquals("local_date_field", gene.fields[0].name)
            assertTrue(gene.fields[0] is DateGene)
        }
    }

    @Test
    fun testLocalDateTimeField() {

        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "local_date_time_field":{
                        "type":"string", 
                        "format":"local-date-time"
                    }
                },
                "required": [
                    "local_date_time_field"
                    
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(1, gene.fields.size)
            assertEquals("local_date_time_field", gene.fields[0].name)
            assertTrue(gene.fields[0] is DateTimeGene)
        }

        val rand = Randomness()

        gene.doInitialize(rand)

        val dateTimeString = gene.fields[0].getValueAsRawString()
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(dateTimeString)
    }


    @Test
    fun testLinksBase() {
        val map = loadAndAssertActions("/swagger/artificial/links/links_base.yaml", 2, true)

        val post = map["POST:/users"] as RestCallAction
        assertEquals(1, post.links.size)
        assertTrue(post.links.all { it.canUse() })
    }

    @Test
    fun testLinksRef() {
        val map = loadAndAssertActions("/swagger/artificial/links/links_ref.yaml", 2, true)

        val post = map["POST:/users"] as RestCallAction
        assertEquals(1, post.links.size)
        assertTrue(post.links.all { it.canUse() })
    }


    @Test
    fun testWrongArrayExampleString() {
        val map = loadAndAssertActions(
            "/swagger/artificial/defaultandexamples/wrong_array_example_string.yaml",
            1,
            RestActionBuilderV3.Options(probUseExamples = 1.0))

        val post = map["POST:/v2/foo/{x}"] as RestCallAction
        val path = post.resolvedPath()
        assertTrue(path.contains("xyz"), path)
        assertTrue(path.contains("bar"), path)
        assertTrue(path.contains("hello"), path)
        assertTrue(path.contains("there"), path)
        /*
            those are not 3 different examples, but a single example concatenating them as a single string
         */
    }

    @Test
    fun testWrongArrayExampleInt() {
        val map = loadAndAssertActions(
            "/swagger/artificial/defaultandexamples/wrong_array_example_int.yaml",
            1,
            RestActionBuilderV3.Options(probUseExamples = 1.0))

        val post = map["POST:/v2/foo/{x}"] as RestCallAction
        val path = post.resolvedPath()

        assertEquals("/v2/foo/0", path)
        /*
            unfortunately, it seems the wrong data is silently ignored
         */
    }


    @Test
    fun testBindingPathChildEnum(){

        val path = "/swagger/artificial/binding/binding_path_child_enum.yaml"
        val actions = loadAndAssertActions(path, 2, RestActionBuilderV3.Options(probUseExamples = 1.0))

        val parent = actions["GET:/v2/api/{x}"] as RestCallAction
        val child = actions["GET:/v2/api/{x}/data"] as RestCallAction //using enum

        // only 1 option in the enum
        assertEquals("/v2/api/foo/data", child.resolvedPath())

        parent.bindToSamePathResolution(child)
        assertEquals("/v2/api/foo", parent.resolvedPath())
    }


    @Test
    fun testExamplesPathChildBinding(){

        val path = "/swagger/artificial/defaultandexamples/examples_path_child.yaml"
        val actions = loadAndAssertActions(path, 2, RestActionBuilderV3.Options(probUseExamples = 1.0))

        val parent = actions["GET:/v2/api/{x}"] as RestCallAction
        val child = actions["GET:/v2/api/{x}/data"] as RestCallAction //has example

        val target = "foo"

        val x = child.parameters.find { it is PathParam }!!.primaryGene().getWrappedGene(ChoiceGene::class.java)!!
        val isSet = x.unsafeSetFromStringValue(target)
        assertTrue(isSet)

        parent.bindToSamePathResolution(child)
        assertEquals("/v2/api/$target", parent.resolvedPath())
    }

    @Test
    fun testExamplesPathParentBinding(){

        val path = "/swagger/artificial/defaultandexamples/examples_path_parent.yaml"
        val actions = loadAndAssertActions(path, 2, RestActionBuilderV3.Options(probUseExamples = 1.0))

        val parent = actions["GET:/v2/api/{x}"] as RestCallAction  //has example
        val child = actions["GET:/v2/api/{x}/data"] as RestCallAction

        val target = "foo"

        val x = child.parameters.find { it is PathParam }!!.primaryGene().getWrappedGene(StringGene::class.java)!!
        val isSet = x.unsafeSetFromStringValue(target)
        assertTrue(isSet)

        parent.bindToSamePathResolution(child)
        assertEquals("/v2/api/$target", parent.resolvedPath())
    }

    @Test
    fun testPropertyStringTypeAndCharFormat(){
        val dtoSchemaName = "foo.com.BarDto"
        val dtoSchema = """
            "$dtoSchemaName":{
                "type":"object",
                "properties": {
                    "char_field":{
                        "type":"string", 
                        "format":"char"
                    }
                },
                "required": [
                    "char_field"
                    
                ]
            }
            """.trimIndent()

        val allSchemas = "\"${dtoSchemaName}\":{${dtoSchema}}"

        val gene = RestActionBuilderV3.createGeneForDTO(
            dtoSchemaName,
            allSchemas,
            RestActionBuilderV3.Options(enableConstraintHandling = true)
        )

        assertTrue(gene is ObjectGene)
        (gene as ObjectGene).apply {
            assertEquals(1, gene.fields.size)
            assertEquals("char_field", gene.fields[0].name)
            assertTrue(gene.fields[0] is StringGene)
            val stringGene = gene.fields[0] as StringGene
            assertEquals(1, stringGene.minLength)
            assertEquals(1, stringGene.maxLength)
        }
    }


    @Test
    fun testRefLinkRef() {
        val map = loadAndAssertActions("/swagger/artificial/ref/linkref.yaml", 2, true)

        val post = map["POST:/users"] as RestCallAction
        assertEquals(1, post.links.size)
        assertTrue(post.links.all { it.canUse() })
    }

    @Test
    fun testRefCycle() {
        val map = loadAndAssertActions("/swagger/artificial/ref/cycleA.yaml", 1, true)

        val get = map["GET:/users/{id}"] as RestCallAction
        assertTrue(get.produces.any { it.contains("xml") })
        assertEquals(1, get.parameters.size)
    }

    @Test
    fun testPathItem(){
        val map = loadAndAssertActions(
            "/swagger/artificial/ref/pathitem.yaml",
            1,
            RestActionBuilderV3.Options(probUseExamples = 1.0))

        val get = map["GET:/users/{id}"] as RestCallAction
        assertEquals(1, get.parameters.size)

        val certain = map.values.first()
            .seeTopGenes().first()
        val output = certain.getValueAsRawString()
        assertEquals("FOO", output)
    }

}
