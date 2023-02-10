package org.evomaster.core.search.gene

import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.search.gene.collection.*
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.search.gene.sql.*
import org.evomaster.core.search.gene.sql.geometric.*
import org.evomaster.core.search.gene.network.CidrGene
import org.evomaster.core.search.gene.network.InetGene
import org.evomaster.core.search.gene.network.MacAddrGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.optional.*
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.sql.textsearch.SqlTextSearchQueryGene
import org.evomaster.core.search.gene.sql.textsearch.SqlTextSearchVectorGene
import org.evomaster.core.search.gene.sql.time.SqlTimeIntervalGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.NumericStringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.uri.UriDataGene
import org.evomaster.core.search.gene.uri.UriGene
import org.evomaster.core.search.gene.uri.UrlHttpGene
import org.evomaster.core.search.gene.utils.NumberMutatorUtils
import org.evomaster.core.search.service.Randomness
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

object GeneSamplerForTests {

    val geneClasses: List<KClass<out Gene>> = loadAllGeneClasses()

    private fun loadAllGeneClasses(): List<KClass<out Gene>> {

        val genes = mutableListOf<KClass<out Gene>>()
        /*
                    Load all the classes that extends from Gene
                 */
        val target = File("target/classes")
        if (!target.exists()) {
            throw IllegalStateException("Compiled class folder does not exist: ${target.absolutePath}")
        }

        target.walk()
                .filter { it.name.endsWith(".class") }
                .map {
                    val s = it.path.replace("\\", "/")
                            .replace("target/classes/", "")
                            .replace("/", ".")
                    s.substring(0, s.length - ".class".length)
                }
                .filter { !it.endsWith("\$Companion") }
                .filter { !it.contains("$") }
                .forEach {
                    //println("Analyzing $it")
                    val c = try {
                        this.javaClass.classLoader.loadClass(it).kotlin
                    } catch (e: Exception) {
                        println("Failed to load class: ${e.message}")
                        throw e
                    }
                    val subclass: Boolean = try {
                        Gene::class.isSuperclassOf(c)
                    } catch (e: java.lang.UnsupportedOperationException) {
                        false
                    }
                    if (subclass) {
                        genes.add(c as KClass<out Gene>)
                    }
                }
        return genes
    }


    /**
     * @param rangeDelta represents a min delta between min and max if the gene has
     * this is used for eg, the min size of MapGene is 2, but the max length of the string key gene is 0
     */
    fun <T> sample(klass: KClass<T>, rand: Randomness, rangeDelta: Int? = null): T where T : Gene {

        return when (klass) {
            /*
                Note that here we do NOT randomize the values of genes, but rather
                the (fixed) constraints

                when genes need input genes, we sample those at random as well
             */
            TaintedArrayGene::class -> sampleTaintedArrayGene(rand, rangeDelta) as T
            ArrayGene::class -> sampleArrayGene(rand, rangeDelta) as T
            Base64StringGene::class -> sampleBase64StringGene(rand, rangeDelta) as T
            BigDecimalGene::class -> sampleBigDecimalGene(rand, rangeDelta) as T
            BigIntegerGene::class -> sampleBigIntegerGene(rand, rangeDelta) as T
            BooleanGene::class -> sampleBooleanGene(rand, rangeDelta) as T
            CycleObjectGene::class -> sampleCycleObjectGene(rand, rangeDelta) as T
            CustomMutationRateGene::class -> sampleDisruptiveGene(rand, rangeDelta) as T
            DoubleGene::class -> sampleDoubleGene(rand, rangeDelta) as T
            EnumGene::class -> sampleEnumGene(rand, rangeDelta) as T
            FloatGene::class -> sampleFloatGene(rand, rangeDelta) as T
            ImmutableDataHolderGene::class -> sampleImmutableDataHolderGene(rand, rangeDelta) as T
            IntegerGene::class -> sampleIntegerGene(rand, rangeDelta) as T
            LimitObjectGene::class -> sampleLimitObjectGene(rand, rangeDelta) as T
            LongGene::class -> sampleLongGene(rand, rangeDelta) as T
            FixedMapGene::class -> sampleFixedMapGene(rand, rangeDelta) as T
            FlexibleMapGene::class -> sampleFlexibleMapGene(rand, rangeDelta) as T
            FlexibleGene::class -> samplePrintableFlexibleGene(rand, rangeDelta) as T
            FlexibleCycleObjectGene::class -> sampleFlexibleCycleObjectGene(rand, rangeDelta) as T
            NumericStringGene::class -> sampleNumericStringGene(rand, rangeDelta) as T
            ObjectGene::class -> sampleObjectGene(rand, rangeDelta) as T
            OptionalGene::class -> sampleOptionalGene(rand, rangeDelta) as T
            ChoiceGene::class -> sampleChoiceGene(rand, rangeDelta) as T
            PairGene::class -> samplePairGene(rand, rangeDelta) as T
            SeededGene::class -> sampleSeededGene(rand, rangeDelta) as T
            StringGene::class -> sampleStringGene(rand, rangeDelta) as T
            TupleGene::class -> sampleTupleGene(rand, rangeDelta) as T
            DateGene::class -> sampleDateGene(rand, rangeDelta) as T
            DateTimeGene::class -> sampleDateGene(rand, rangeDelta) as T
            TimeGene::class -> sampleTimeGene(rand, rangeDelta) as T
            AnyCharacterRxGene::class -> sampleAnyCharacterRxGene(rand, rangeDelta) as T
            CharacterClassEscapeRxGene::class -> sampleCharacterClassEscapeRxGene(rand, rangeDelta) as T
            CharacterRangeRxGene::class -> sampleCharacterRangeRxGene(rand, rangeDelta) as T
            DisjunctionRxGene::class -> sampleDisjunctionRxGene(rand, rangeDelta) as T
            DisjunctionListRxGene::class -> sampleDisjunctionListRxGene(rand, rangeDelta) as T
            PatternCharacterBlockGene::class -> samplePatternCharacterBlock(rand, rangeDelta) as T
            QuantifierRxGene::class -> sampleQuantifierRxGene(rand, rangeDelta) as T
            RegexGene::class -> sampleRegexGene(rand, rangeDelta) as T

            //SQL genes
            SqlJSONPathGene::class -> sampleSqlJSONPathGene(rand, rangeDelta) as T
            SqlTextSearchVectorGene::class -> sampleSqlTextSearchVectorGene(rand, rangeDelta) as T
            SqlBoxGene::class -> sampleSqlBoxGene(rand, rangeDelta) as T
            SqlPointGene::class -> sampleSqlPointGene(rand, rangeDelta) as T
            SqlForeignKeyGene::class -> sampleSqlForeignKeyGene(rand, rangeDelta) as T
            SqlLogSeqNumberGene::class -> sampleSqlLogSeqNumberGene(rand, rangeDelta) as T
            SqlRangeGene::class -> sampleSqlRangeGene(rand, rangeDelta) as T
            SqlJSONGene::class -> sampleSqlJSONGene(rand, rangeDelta) as T
            SqlTextSearchQueryGene::class -> sampleSqlTextSearchQueryGene(rand, rangeDelta) as T
            SqlPrimaryKeyGene::class -> sampleSqlPrimaryKeyGene(rand, rangeDelta) as T
            NullableGene::class -> sampleSqlNullableGene(rand, rangeDelta) as T
            SqlMultidimensionalArrayGene::class -> sampleSqlMultidimensionalArrayGene(rand, rangeDelta) as T
            MacAddrGene::class -> sampleSqlMacAddrGene(rand, rangeDelta) as T
            InetGene::class -> sampleSqlInetGene(rand, rangeDelta) as T
            CidrGene::class -> sampleSqlCidrGene(rand, rangeDelta) as T
            SqlAutoIncrementGene::class -> sampleSqlAutoIncrementGene(rand, rangeDelta) as T
            SqlPathGene::class -> sampleSqlPathGene(rand, rangeDelta) as T
            SqlMultiPointGene::class -> sampleSqlMultiPointGene(rand, rangeDelta) as T
            SqlMultiPolygonGene::class -> sampleSqlMultiPolygonGene(rand, rangeDelta) as T
            SqlMultiPathGene::class -> sampleSqlMultiPathGene(rand, rangeDelta) as T
            SqlLineGene::class -> sampleSqlLineGene(rand, rangeDelta) as T
            SqlPolygonGene::class -> sampleSqlPolygonGene(rand, rangeDelta) as T
            SqlCircleGene::class -> sampleSqlCircleGene(rand, rangeDelta) as T
            SqlLineSegmentGene::class -> sampleSqlLineSegmentGene(rand, rangeDelta) as T
            SqlTimeIntervalGene::class -> sampleSqlTimeIntervalGene(rand, rangeDelta) as T
            SqlCompositeGene::class -> sampleSqlCompositeGene(rand, rangeDelta) as T
            SqlBitStringGene::class -> sampleSqlBitStringGene(rand, rangeDelta) as T
            SqlXMLGene::class -> sampleSqlXMLGene(rand, rangeDelta) as T
            SqlMultiRangeGene::class -> sampleSqlMultiRangeGene(rand, rangeDelta) as T
            SqlBinaryStringGene::class -> sampleSqlBinaryStringGene(rand, rangeDelta) as T
            UUIDGene::class -> sampleSqlUUIDGene(rand, rangeDelta) as T
            SqlGeometryCollectionGene::class -> sampleSqlGeometryCollectionGene(rand, rangeDelta) as T
            UriGene::class -> sampleUrlGene(rand, rangeDelta) as T
            UrlHttpGene::class -> sampleUrlHttpGene(rand, rangeDelta) as T
            UriDataGene::class -> sampleUrlDataGene(rand, rangeDelta) as T

            else -> throw IllegalStateException("No sampler for $klass")
        }
    }



    private fun sampleUrlDataGene(rand: Randomness, rangeDelta: Int? = null): UriDataGene {
        return UriDataGene("rand UrlDataGene ${rand.nextInt()}")
    }

    private fun sampleUrlHttpGene(rand: Randomness, rangeDelta: Int? = null): UrlHttpGene {
        return UrlHttpGene("rand UrlHttpGene ${rand.nextInt()}")
    }

    private fun sampleUrlGene(rand: Randomness, rangeDelta: Int? = null): UriGene {
        return UriGene("rand UrlGene ${rand.nextInt()}")
    }

    private fun sampleSqlUUIDGene(rand: Randomness, rangeDelta: Int? = null): UUIDGene {
        return UUIDGene("rand SqlUUIDGene ${rand.nextInt()}")
    }

    private fun sampleSqlBinaryStringGene(rand: Randomness, rangeDelta: Int? = null): SqlBinaryStringGene {
        val maxSize = rand.nextInt(max(1, rangeDelta?:0), ArrayGene.MAX_SIZE)
        val minSize = rand.nextInt(0, maxSize)
        return SqlBinaryStringGene("rand SqlBinaryStringGene",
                minSize = minSize,
                maxSize = maxSize)
    }

    private fun sampleSqlMultiRangeGene(rand: Randomness, rangeDelta: Int? = null): SqlMultiRangeGene<*> {
        return SqlMultiRangeGene("rand SqlMultiRangeGene", template = sampleSqlRangeGene(rand, rangeDelta))
    }

    private fun sampleSqlXMLGene(rand: Randomness, rangeDelta: Int? = null): SqlXMLGene {
        return SqlXMLGene("rand SqlXMLGene ${rand.nextInt()}")
    }

    private fun sampleSqlBitStringGene(rand: Randomness, rangeDelta: Int? = null): SqlBitStringGene {
        val maxSize = rand.nextInt(1, ArrayGene.MAX_SIZE)
        val minSize = rand.nextInt(0, maxSize)
        return SqlBitStringGene("rand SqlBitStringGene",
                minSize = minSize,
                maxSize = maxSize)
    }

    private fun sampleSqlCompositeGene(rand: Randomness, rangeDelta: Int? = null): SqlCompositeGene {
        val selection = geneClasses.filter { !it.isAbstract }

        val numberOfFields = rand.nextInt(1, MAX_NUMBER_OF_FIELDS)
        return SqlCompositeGene(
                name = "rand SqlCompositeGene",
                fields = List(numberOfFields) { sample(rand.choose(selection), rand, rangeDelta) }
        )
    }

    private fun sampleSqlTimeIntervalGene(rand: Randomness, rangeDelta: Int? = null): SqlTimeIntervalGene {
        val timeGeneFormats = listOf(TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT,
                TimeGene.TimeGeneFormat.TIME_WITH_MILLISECONDS)
        val timeGeneFormat = rand.choose(timeGeneFormats)
        return SqlTimeIntervalGene("rand SqlTimeIntervalGene",
                time = TimeGene("hoursMinutesAndSeconds", timeGeneFormat = timeGeneFormat))
    }

    private fun sampleSqlLineSegmentGene(rand: Randomness, rangeDelta: Int? = null): SqlLineSegmentGene {
        return SqlLineSegmentGene("rand SqlLineSegmentGene ${rand.nextInt()}")
    }

    private fun sampleSqlCircleGene(rand: Randomness, rangeDelta: Int? = null): SqlCircleGene {
        return SqlCircleGene("rand SqlCircleGene ${rand.nextInt()}")
    }

    private fun sampleSqlPolygonGene(rand: Randomness, rangeDelta: Int? = null): SqlPolygonGene {
        return SqlPolygonGene("rand SqlPolygonGene ${rand.nextInt()}")
    }

    private fun sampleSqlLineGene(rand: Randomness, rangeDelta: Int? = null): SqlLineGene {
        return SqlLineGene("rand SqlLineGene ${rand.nextInt()}")
    }

    private fun sampleSqlPathGene(rand: Randomness, rangeDelta: Int? = null): SqlPathGene {
        return SqlPathGene("rand SqlPathGene ${rand.nextInt()}")
    }

    private fun sampleSqlMultiPointGene(rand: Randomness, rangeDelta: Int? = null) = SqlMultiPointGene("rand SqlMultiPointGene ${rand.nextInt()}")


    private fun sampleSqlGeometryCollectionGene(rand: Randomness, range: Int? = null): SqlGeometryCollectionGene {
        return SqlGeometryCollectionGene("rand SqlGeometryCollectionGene ${rand.nextInt()}",
                template = ChoiceGene(name = "rand ChoiceGene", listOf<Gene>(sample(SqlPointGene::class, rand, range),
                        sample(SqlPathGene::class, rand, range), sample(SqlMultiPointGene::class, rand, range),
                        sample(SqlMultiPathGene::class, rand, range),
                        sample(SqlPolygonGene::class, rand, range),
                        sample(SqlMultiPolygonGene::class, rand, range)))
        )
    }

    private fun sampleSqlMultiPolygonGene(rand: Randomness, rangeDelta: Int? = null): SqlMultiPolygonGene {
        return SqlMultiPolygonGene("rand SqlMultiPolygonGene ${rand.nextInt()}")
    }

    private fun sampleSqlMultiPathGene(rand: Randomness, rangeDelta: Int? = null): SqlMultiPathGene {
        return SqlMultiPathGene("rand SqlMultiPathGene ${rand.nextInt()}")
    }

    private fun sampleSqlAutoIncrementGene(rand: Randomness, rangeDelta: Int? = null): SqlAutoIncrementGene {
        return SqlAutoIncrementGene("rand SqlAutoIncrementGene ${rand.nextInt()}")
    }

    private fun sampleSqlCidrGene(rand: Randomness, rangeDelta: Int? = null): CidrGene {
        return CidrGene("rand SqlCidrGene ${rand.nextInt()}")
    }

    private fun sampleSqlInetGene(rand: Randomness, rangeDelta: Int? = null): InetGene {
        return InetGene("rand SqlInetGene ${rand.nextInt()}")
    }

    private fun sampleSqlMacAddrGene(rand: Randomness, rangeDelta: Int? = null): MacAddrGene {
        return MacAddrGene("rand SqlMacAddrGene ${rand.nextInt()}",
                numberOfOctets = rand.nextInt(1, MAX_NUMBER_OF_OCTETS))
    }

    const val MAX_NUMBER_OF_DIMENSIONS = 5
    const val MAX_NUMBER_OF_OCTETS = 10
    const val MAX_NUMBER_OF_FIELDS = 3

    private fun selectionForArrayTemplate(): List<KClass<out Gene>> {
        return geneClasses
                .filter { !it.isAbstract }
                .filter { it.java != CycleObjectGene::class.java && it.java !== LimitObjectGene::class.java }
                .filter { it.java != ArrayGene::class.java && it.java != SqlMultidimensionalArrayGene::class.java }
        // TODO might filter out some more genes here
    }

    private fun sampleSqlMultidimensionalArrayGene(rand: Randomness, rangeDelta: Int? = null): SqlMultidimensionalArrayGene<*> {

        val selection = selectionForArrayTemplate()
        val template = samplePrintableTemplate(selection, rand, rangeDelta)

        return SqlMultidimensionalArrayGene("rand SqlMultidimensionalArrayGene",
                template = template,
                numberOfDimensions = rand.nextInt(1, MAX_NUMBER_OF_DIMENSIONS))
    }

    private fun sampleSqlNullableGene(rand: Randomness, rangeDelta: Int? = null): NullableGene {
        val selection = geneClasses.filter { !it.isAbstract }
                .filter { it.java != SqlForeignKeyGene::class.java }
        return NullableGene("rand NullableGene",
                gene = sample(rand.choose(selection), rand, rangeDelta))
    }

    private fun sampleSqlPrimaryKeyGene(rand: Randomness, rangeDelta: Int? = null): SqlPrimaryKeyGene {
        val selection = geneClasses.filter { !it.isAbstract && it.isSubclassOf(ComparableGene::class) }

        return SqlPrimaryKeyGene("rand SqlPrimaryKeyGene",
                tableName = "rand tableName",
                gene = sample(rand.choose(selection), rand, rangeDelta),
                uniqueId = rand.nextLong(0, Long.MAX_VALUE))
    }

    private fun sampleSqlTextSearchQueryGene(rand: Randomness, rangeDelta: Int? = null): SqlTextSearchQueryGene {
        return SqlTextSearchQueryGene("rand SqlTextSearchQueryGene ${rand.nextInt()}")
    }

    private fun sampleSqlJSONGene(rand: Randomness, rangeDelta: Int? = null): SqlJSONGene {
        return SqlJSONGene("rand SqlJSONGene ${rand.nextInt()}")
    }

    private fun sampleSqlRangeGene(rand: Randomness, rangeDelta: Int? = null): SqlRangeGene<*> {
        val selection = geneClasses.filter { !it.isAbstract && it.isSubclassOf(ComparableGene::class) }
        val selectedClass = rand.choose(selection)
        val templateSample = sample(selectedClass, rand, rangeDelta)
        if (templateSample !is ComparableGene) {
            throw IllegalStateException("${templateSample::class.java} does not implement ComparableGene")
        }
        return SqlRangeGene(
                "rand SqlRangeGene",
                template = templateSample)

    }

    private fun sampleSqlLogSeqNumberGene(rand: Randomness, rangeDelta: Int? = null): SqlLogSeqNumberGene {
        return SqlLogSeqNumberGene("rand SqlLogSeqNumberGene ${rand.nextInt()}")
    }

    private fun sampleSqlForeignKeyGene(rand: Randomness, rangeDelta: Int? = null): SqlForeignKeyGene {
        return SqlForeignKeyGene(sourceColumn = "rand source column",
                uniqueId = rand.nextLong(min = 0L, max = Long.MAX_VALUE),
                targetTable = "rand target table",
                nullable = rand.nextBoolean(),
                uniqueIdOfPrimaryKey = rand.nextLong())
    }

    private fun sampleSqlPointGene(rand: Randomness, rangeDelta: Int? = null): SqlPointGene {
        return SqlPointGene("rand SqlPointGene ${rand.nextInt()}")
    }

    private fun sampleSqlBoxGene(rand: Randomness, rangeDelta: Int? = null): SqlBoxGene {
        return SqlBoxGene("rand SqlBoxGene ${rand.nextInt()}")
    }

    private fun sampleSqlTextSearchVectorGene(rand: Randomness, rangeDelta: Int? = null): SqlTextSearchVectorGene {
        return SqlTextSearchVectorGene("rand SqlTextSearchVectorGene ${rand.nextInt()}")

    }

    private fun sampleSqlJSONPathGene(rand: Randomness, rangeDelta: Int? = null): SqlJSONPathGene {
        return SqlJSONPathGene("rand JSONPathGene ${rand.nextInt()}")
    }

    fun sampleRegexGene(rand: Randomness, rangeDelta: Int? = null): RegexGene {
        return RegexGene(name = "rand RegexGene", disjunctions = sampleDisjunctionListRxGene(rand, rangeDelta))
    }

    fun sampleQuantifierRxGene(rand: Randomness, rangeDelta: Int? = null): QuantifierRxGene {

        val selection = geneClasses
                .filter { !it.isAbstract }
                .filter { it.isSubclassOf(RxAtom::class) }
        val min = rand.nextInt(2)

        return QuantifierRxGene(
                name = "rand QuantifierRxGene",
                template = sample(rand.choose(selection), rand, rangeDelta),
                min = min,
                max = min + rand.nextInt(1, 2)
        )
    }

    fun samplePatternCharacterBlock(rand: Randomness, rangeDelta: Int? = null): PatternCharacterBlockGene {
        return PatternCharacterBlockGene(name = "rand PatternCharacterBlock", stringBlock = rand.nextWordString())
    }

    fun sampleDisjunctionListRxGene(rand: Randomness, rangeDelta: Int? = null): DisjunctionListRxGene {

        return DisjunctionListRxGene(listOf(
                sampleDisjunctionRxGene(rand, rangeDelta),
                sampleDisjunctionRxGene(rand, rangeDelta)
        ))
    }

    fun sampleDisjunctionRxGene(rand: Randomness, rangeDelta: Int? = null): DisjunctionRxGene {

        val selection = geneClasses
                .filter { !it.isAbstract }
                .filter { it.isSubclassOf(RxTerm::class) }
                //let's avoid huge trees...
                .filter {
                    (it.java != DisjunctionListRxGene::class.java && it.java != DisjunctionRxGene::class.java)
                            || rand.nextBoolean()
                }

        val numberOfTerms = rand.nextInt(1, 3)
        return DisjunctionRxGene(
                name = "rand DisjunctionRxGene",
                terms = List(numberOfTerms) { sample(rand.choose(selection), rand, rangeDelta) },
                matchStart = rand.nextBoolean(),
                matchEnd = rand.nextBoolean()
        )
    }

    fun sampleCharacterRangeRxGene(rand: Randomness, rangeDelta: Int? = null): CharacterRangeRxGene {
        return CharacterRangeRxGene(
                negated = false, // TODO update once fixed
                ranges = listOf(Pair('a', 'z'))
        )
    }

    fun sampleCharacterClassEscapeRxGene(rand: Randomness, rangeDelta: Int? = null): CharacterClassEscapeRxGene {
        return CharacterClassEscapeRxGene(type = rand.choose(listOf("w", "W", "d", "D", "s", "S")))
    }

    fun sampleAnyCharacterRxGene(rand: Randomness, rangeDelta: Int? = null): AnyCharacterRxGene {
        return AnyCharacterRxGene()
    }

    fun sampleTimeGene(rand: Randomness, rangeDelta: Int? = null): TimeGene {
        return TimeGene(name = "rand TimeGene")
    }

    fun sampleDateTimeGene(rand: Randomness, rangeDelta: Int? = null): DateTimeGene {
        return DateTimeGene("rand DateTimeGene")
    }

    fun sampleDateGene(rand: Randomness, rangeDelta: Int? = null): DateGene {
        return DateGene(name = "rand DateGene", onlyValidDates = rand.nextBoolean())
    }

    fun sampleSeededGene(rand: Randomness, rangeDelta: Int? = null): SeededGene<*> {

        //TODO update after refactoring SeededGene with ChoiceGene (to implement)

        return SeededGene(
                name = "rand SeededGene",
                gene = sampleStringGene(rand, rangeDelta),
                seeded = sampleEnumGene(rand, rangeDelta) as EnumGene<StringGene>,
                employSeeded = rand.nextBoolean()
        )
    }

    fun sampleTupleGene(rand: Randomness, rangeDelta: Int? = null): TupleGene {

        val selection = geneClasses.filter { !it.isAbstract }

        return TupleGene(
                name = "rand TupleGene ${rand.nextInt()}",
                elements = listOf(
                        sample(rand.choose(selection), rand, rangeDelta),
                        sample(rand.choose(selection), rand, rangeDelta),
                        sample(rand.choose(selection), rand, rangeDelta)
                ),
                lastElementTreatedSpecially = rand.nextBoolean()

        )
    }

    fun samplePairGene(rand: Randomness, rangeDelta: Int? = null): PairGene<*, *> {

        val selection = geneClasses.filter { !it.isAbstract }

        return PairGene(
                name = "rand PairGene",
                first = sample(rand.choose(selection), rand, rangeDelta),
                second = sample(rand.choose(selection), rand, rangeDelta),
                isFirstMutable = rand.nextBoolean()
        )
    }

    fun samplePrintablePairGene(rand: Randomness, rangeDelta: Int? = null): PairGene<*, *> {

        val selection = geneClasses.filter { !it.isAbstract }

        return PairGene(
            name = "rand PairGene",
            first = samplePrintableTemplate(selection, rand, rangeDelta),
            second = samplePrintableTemplate(selection, rand, rangeDelta),
            isFirstMutable = rand.nextBoolean()
        )
    }

    fun samplePrintableFlexiblePairGene(rand: Randomness, rangeDelta: Int? = null): PairGene<*, FlexibleGene> {

        val selection = geneClasses.filter { !it.isAbstract }

        return PairGene(
            name = "rand PairGene",
            first = samplePrintableTemplate(selection, rand, rangeDelta),
            second = samplePrintableFlexibleGene(rand, rangeDelta),
            isFirstMutable = rand.nextBoolean()
        )
    }
    fun samplePrintableFlexibleGene(rand: Randomness, rangeDelta: Int? = null): FlexibleGene {

        val selection = geneClasses.filter { !it.isAbstract}

        val valueTemplate = samplePrintableTemplate(selection, rand, rangeDelta)
        return FlexibleGene(valueTemplate.name, valueTemplate)
    }

    fun sampleFlexibleCycleObjectGene(rand: Randomness, rangeDelta: Int? = null) : FlexibleCycleObjectGene{
        val gene = sampleCycleObjectGene(rand, rangeDelta)
        return FlexibleCycleObjectGene(gene.name, gene)
    }

    fun sampleOptionalGene(rand: Randomness, rangeDelta: Int? = null): OptionalGene {

        val selection = geneClasses.filter { !it.isAbstract }

        return OptionalGene(
                name = "rand OptionalGene",
                gene = sample(rand.choose(selection), rand, rangeDelta)
        )
    }

    fun sampleChoiceGene(rand: Randomness, rangeDelta: Int? = null): ChoiceGene<*> {
        val selection = geneClasses.filter { !it.isAbstract }
        return ChoiceGene<Gene>(
                name = "rand ChoiceGene",
                geneChoices = listOf(
                        sample(rand.choose(selection), rand, rangeDelta),
                        sample(rand.choose(selection), rand, rangeDelta)
                )
        )
    }

    fun sampleObjectGene(rand: Randomness, rangeDelta: Int? = null): ObjectGene {

        val selection = geneClasses.filter { !it.isAbstract }

        return ObjectGene(
                name = "rand ObjectGene ${rand.nextInt()}",
                fields = listOf(
                        sample(rand.choose(selection), rand, rangeDelta),
                        sample(rand.choose(selection), rand, rangeDelta),
                        sample(rand.choose(selection), rand, rangeDelta)
                )
        )
    }

    fun sampleNumericStringGene(rand: Randomness, rangeDelta: Int? = null): NumericStringGene {
        return NumericStringGene(
                name = "rand NumericStringGene",
                minLength = rand.nextInt(2),
                number = sample(BigDecimalGene::class, rand, rangeDelta)
        )
    }

    fun sampleFixedMapGene(rand: Randomness, rangeDelta: Int? = null): FixedMapGene<*, *> {

        val min = rand.nextInt(0, 2)
        val minSize = rand.choose(listOf(null, min))
        val maxSize = rand.choose(listOf(null, min + (rangeDelta?:0) + rand.nextInt(1, 3)))

        return FixedMapGene(
                name = "rand MapGene",
                minSize = minSize,
                maxSize = maxSize,
                template = samplePrintablePairGene(rand, if (minSize != null) max(minSize, rangeDelta?:0) else rangeDelta)
        )
    }

    fun sampleFlexibleMapGene(rand: Randomness, rangeDelta: Int? = null): FlexibleMapGene<*> {

        val min = rand.nextInt(0, 2)
        val minSize = rand.choose(listOf(null, min))
        val maxSize = rand.choose(listOf(null, min + (rangeDelta?:0) + rand.nextInt(1, 3)))

        return FlexibleMapGene(
            name = "rand MapGene",
            minSize = minSize,
            maxSize = maxSize,
            template = samplePrintableFlexiblePairGene(rand, if (minSize != null) max(minSize, rangeDelta?:0) else rangeDelta)
        )
    }

    fun sampleLimitObjectGene(rand: Randomness, rangeDelta: Int? = null): LimitObjectGene {
        return LimitObjectGene(name = "rand LimitObjectGene")
    }

    fun sampleImmutableDataHolderGene(rand: Randomness, rangeDelta: Int? = null): ImmutableDataHolderGene {
        return ImmutableDataHolderGene(
                name = "rand ImmutableDataHolderGene",
                value = rand.nextWordString(),
                inQuotes = rand.nextBoolean()
        )
    }

    fun sampleEnumGene(rand: Randomness, rangeDelta: Int? = null): EnumGene<*> {
        return EnumGene<String>("rand EnumGene ${rand.nextInt()}", listOf("A", "B", "C"))
    }


    fun sampleDisruptiveGene(rand: Randomness, rangeDelta: Int? = null): CustomMutationRateGene<*> {
        val selection = geneClasses
                .filter { !it.isAbstract }
                .filter { it != CustomMutationRateGene::class }
        val chosen = sample(rand.choose(selection), rand, rangeDelta)

        return CustomMutationRateGene("rand DisruptiveGene", chosen, 0.5)
    }

    fun sampleCycleObjectGene(rand: Randomness, rangeDelta: Int? = null): CycleObjectGene {
        return CycleObjectGene("rand CycleObjectGene ${rand.nextInt()}")
    }


    fun sampleBooleanGene(rand: Randomness, rangeDelta: Int? = null): BooleanGene {
        return BooleanGene(name = "rand boolean ${rand.nextInt()}")
    }

    fun sampleDoubleGene(rand: Randomness, rangeDelta: Int? = null): DoubleGene {
        val scale: Int? = rand.choose(listOf(null, rand.nextInt(0, 2)))

        // if scale is 0, to distinguish min and max
        val min = rand.nextDouble().run {
            // format min based on scale with 50%
            if (rand.nextBoolean())
                NumberMutatorUtils.getFormattedValue(this, scale, RoundingMode.UP)
            else
                this
        }

        val least = getMinPrecision(min)
        val precision = max(min(least + rand.nextInt(0, 10), 308), least) + (scale ?: 0)

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()

        val actualScale = getScale(min)
        val delta: Double = (if (!minInclusive || !maxInclusive) 2.0 else 0.0).run { if (scale != null && actualScale > scale) this + 2.0 else this }

        return DoubleGene(
                name = "rand DoubleGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + max((rangeDelta?:0).toDouble(), delta)+ rand.nextDouble())),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
                scale = scale
        )
    }


    fun sampleIntegerGene(rand: Randomness, rangeDelta: Int? = null): IntegerGene {
        val min = rand.nextInt() / 2

        val least = max(getMinPrecision(min), rangeDelta?:0)
        val precision = max(min(least + rand.nextInt(0, 2), 8), least)

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()
        val delta = if (!minInclusive && !maxInclusive) 3 // consider randomize with new value, we might employ delta 3 instead of 2
        else if (!minInclusive || !maxInclusive) 2 else 0


        return IntegerGene(
                name = "rand IntegerGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + delta + rand.nextInt(0, 100))),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
        )
    }

    fun sampleLongGene(rand: Randomness, rangeDelta: Int? = null): LongGene {
        val min = rand.nextLong() / 2

        val least = max(getMinPrecision(min), rangeDelta?:0)
        val precision = max(min(least + rand.nextInt(0, 2), 10), least)


        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()
        val minDelta = if (!minInclusive && !maxInclusive) 3 else 2

        return LongGene(
                name = "rand LongGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + rand.nextInt(minDelta, 100))),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
        )
    }

    fun sampleFloatGene(rand: Randomness, rangeDelta: Int? = null): FloatGene {
        val scale: Int? = rand.choose(listOf(null, rand.nextInt(0, 2)))

        val min = rand.nextFloat().run {
            // format min based on scale with 50%
            if (rand.nextBoolean())
                NumberMutatorUtils.getFormattedValue(this, scale, RoundingMode.UP)
            else
                this
        }

        val least = max(getMinPrecision(min), rangeDelta?:0)

        val precision = max(min(least + rand.nextInt(0, 2), 12), least) + (scale ?: 0)

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()

        val actualScale = getScale(min)
        val delta: Float = (if (!minInclusive || !maxInclusive) 2.0f else 0.0f).run { if (scale != null && actualScale > scale) this + 2.0f else this }

        return FloatGene(
                name = "rand FloatGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + delta + abs(rand.nextFloat()))),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
                scale = scale
        )
    }

    fun sampleBigDecimalGene(rand: Randomness, rangeDelta: Int? = null): BigDecimalGene {

        val scale: Int? = rand.choose(listOf(null, rand.nextInt(0, 2)))

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()

        val minBigDecimal: BigDecimal?
        val maxBigDecimal: BigDecimal?
        if (rand.nextBoolean()) {
            minBigDecimal = null
            maxBigDecimal = null
        } else {
            val min = rand.nextLong() / 2
            minBigDecimal = BigDecimal.valueOf(min)

            val minDelta: Long = if (!minInclusive && !maxInclusive) 3 else 2
            val addition = if (minBigDecimal.toDouble() >= 0) BigDecimal.valueOf(Long.MAX_VALUE).subtract(minBigDecimal).toLong() else Long.MAX_VALUE
            maxBigDecimal = minBigDecimal + BigDecimal.valueOf(max(minDelta, rand.nextLong(0, addition) / 2))
        }

        val least = max((if (minBigDecimal != null) getMinPrecision(minBigDecimal) else rand.nextInt(1, 5)), rangeDelta?:0)

        val precision = max(min(least + rand.nextInt(0, 2), 12), least) + (scale ?: 0)

        return BigDecimalGene(
                name = "rand BigDecimalGene ${rand.nextInt()}",
                min = minBigDecimal,
                max = maxBigDecimal,
                minInclusive = if (minBigDecimal == null) true else minInclusive,
                maxInclusive = if (maxBigDecimal == null) true else maxInclusive,
                floatingPointMode = rand.nextBoolean(),
                precision = rand.choose(listOf(null, precision)),
                scale = scale
        )
    }

    fun sampleBigIntegerGene(rand: Randomness, rangeDelta: Int? = null): BigIntegerGene {
        val minBigInteger: BigInteger?
        val maxBigInteger: BigInteger?

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()
        val minDelta = if (!minInclusive && !maxInclusive) 3L else 2L

        if (rand.nextBoolean()) {
            minBigInteger = null
            maxBigInteger = null
        } else {
            minBigInteger = BigInteger.valueOf(rand.nextLong() / 2)
            maxBigInteger = minBigInteger.plus(BigInteger.valueOf(max(minDelta, rand.nextLong(0, Long.MAX_VALUE) / 2)))
        }

        val least = max(if (minBigInteger != null) getMinPrecision(minBigInteger) else rand.nextInt(1, 5), rangeDelta?:0)
        val precision = max(min(least + rand.nextInt(0, 2), 12), least)

        return BigIntegerGene(
                name = "rand BigIntegerGene ${rand.nextInt()}",
                min = minBigInteger,
                max = maxBigInteger,
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
        )
    }

    private fun samplePrintableTemplate(selection: List<KClass<out Gene>>, rand: Randomness, rangeDelta: Int? = null) : Gene{
        val filter = selection.filter { !isFlexible(it) }
        var chosen = sample(rand.choose(filter), rand, rangeDelta)
        while(!chosen.isPrintable()){
            chosen = sample(rand.choose(filter), rand, rangeDelta)
        }
        return chosen
    }

    private fun isFlexible(kclass : KClass<out Gene>) : Boolean = kclass == FlexibleGene::class || kclass == FlexibleCycleObjectGene::class || kclass == FlexibleMapGene::class

    private fun sampleTaintedArrayGene(rand: Randomness, rangeDelta: Int? = null): TaintedArrayGene {

        val array = if(rand.nextBoolean()) sampleArrayGene(rand, rangeDelta) else null
        val isActive = if(array == null) false else rand.nextBoolean()

        return TaintedArrayGene(
            "tainted array ${rand.nextInt()}",
            TaintInputName.getTaintName(rand.nextInt(0,1000)),
            isActive,
            array
        )
    }

    fun sampleArrayGene(rand: Randomness, rangeDelta: Int? = null): ArrayGene<*> {

        val selection = selectionForArrayTemplate()
        val chosen = samplePrintableTemplate(selection, rand, rangeDelta)

        return ArrayGene("rand array ${rand.nextInt()}", chosen)
    }

    fun sampleBase64StringGene(rand: Randomness, rangeDelta: Int? = null): Base64StringGene {
        return Base64StringGene("rand Base64StringGene ${rand.nextInt()}")
    }

    fun sampleStringGene(rand: Randomness, rangeDelta: Int? = null): StringGene {

        val min = rand.nextInt(0, 3)
        val max = min + rand.nextInt(rangeDelta?:0,20)

        return StringGene("rand string ${rand.nextInt()}", minLength = min, maxLength = max)
    }

    private fun getMinPrecision(value: Number): Int {
        return value.toString().split(".")[0].replace("-", "").length
    }

    private fun getScale(value: Number): Int {
        return value.toString().run {
            if (!contains(".")) 0
            else split(".")[1].length
        }
    }

}