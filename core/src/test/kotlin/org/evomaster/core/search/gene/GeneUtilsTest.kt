package org.evomaster.core.search.gene

import org.evomaster.core.sql.SqlActionGeneBuilder
import org.evomaster.core.problem.graphql.GqlConst
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.builder.GraphQLActionBuilder
import org.evomaster.core.problem.graphql.PetClinicCheckMain
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class GeneUtilsTest {

    @Test
    fun testPadding() {

        val x = 9
        val res = GeneUtils.padded(x, 2)

        assertEquals("09", res)
    }


    @Test
    fun testPaddingNegative() {

        val x = -2
        val res = GeneUtils.padded(x, 3)

        assertEquals("-02", res)

    }

    @Test
    fun testRepairDefaultDateGene() {
        val gene = DateGene("date")
        GeneUtils.repairGenes(listOf(gene))
        GregorianCalendar(gene.year.value, gene.month.value, gene.day.value, 0, 0)
    }

    @Test
    fun testRepairBrokenDateGene() {
        val gene = DateGene("date", IntegerGene("year", 1998), IntegerGene("month", 4), IntegerGene("day", 31))
        GeneUtils.repairGenes(gene.flatView())
        GregorianCalendar(gene.year.value, gene.month.value, gene.day.value, 0, 0)
        assertEquals(1998, gene.year.value)
        assertEquals(4, gene.month.value)
        assertEquals(30, gene.day.value)
    }

    @Test
    fun testRepairBrokenSqlTimestampGene() {
        val sqlTimestampGene = SqlActionGeneBuilder().buildSqlTimestampGene("timestamp")
        sqlTimestampGene.date.apply {
            year.value = 1998
            month.value = 4
            day.value = 31
        }
        sqlTimestampGene.time.apply {
            hour.value = 23
            minute.value = 13
            second.value = 41
        }

        GeneUtils.repairGenes(sqlTimestampGene.flatView())
        sqlTimestampGene.apply {
            GregorianCalendar(this.date.year.value, this.date.month.value, this.date.day.value, this.time.hour.value, this.time.minute.value)
        }
        sqlTimestampGene.date.apply {
            assertEquals(1998, this.year.value)
            assertEquals(4, this.month.value)
            assertEquals(30, this.day.value)
        }
        sqlTimestampGene.time.apply {
            assertEquals(23, this.hour.value)
            assertEquals(13, this.minute.value)
            assertEquals(41, this.second.value)
        }
    }

    @Test
    fun testFlatViewWithExcludeDateGene() {
        val sqlTimestampGene = SqlActionGeneBuilder().buildSqlTimestampGene("timestamp")
        sqlTimestampGene.date.apply {
            year.value = 1998
            month.value = 4
            day.value = 31
        }
        sqlTimestampGene.time.apply {
            hour.value = 23
            minute.value = 13
            second.value = 41
        }
        val excludePredicate = { gene: Gene -> (gene is DateGene) }
        sqlTimestampGene.flatView(excludePredicate).apply {
            assertFalse(contains(sqlTimestampGene.date.year))
            assertFalse(contains(sqlTimestampGene.date.month))
            assertFalse(contains(sqlTimestampGene.date.day))
            assert(contains(sqlTimestampGene.date))
            assert(contains(sqlTimestampGene.time))
            assert(contains(sqlTimestampGene.time.hour))
            assert(contains(sqlTimestampGene.time.minute))
            assert(contains(sqlTimestampGene.time.second))
        }
    }


    @Test
    fun testBooleanSelectionSimple() {

        val obj = ObjectGene("foo", listOf(StringGene("a", "hello"), IntegerGene("b", 42)))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        //without randomization, should be both on by default
        assertEquals("{a,b}", rep)
    }

    @Test
    fun testBooleanSectionSkip() {

        val obj = ObjectGene("foo", listOf(OptionalGene("a", StringGene("a", "hello")), IntegerGene("b", 42)))

        val selection = GeneUtils.getBooleanSelection(obj)

        val a = selection.fields.find { it.name == "a" } as BooleanGene
        a.value = false

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{b}", rep)
    }


    @Test
    fun testBooleanSectionTwoNestedObjects() {

        val obj = ObjectGene("Obj1", listOf(ObjectGene("Obj2", listOf(StringGene("a", "hello")))))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{Obj2{a}}", rep)
    }

    @Test
    fun testBooleanSectionThreeNestedObjects() {

        val obj = ObjectGene("Obj1", listOf(ObjectGene("Obj2", listOf(ObjectGene("Obj3", listOf(StringGene("a", "hello")))))))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{Obj2{Obj3{a}}}", rep)
    }


    @Test
    fun testBooleanSectionArray() {

        val array = ArrayGene("Array", ObjectGene("Obj1", listOf(StringGene("a", "hello"))))

        val selection = GeneUtils.getBooleanSelection(array)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testBooleanSectionString() {

        val string = StringGene("a", "hello")
        val obj = ObjectGene("obj", listOf(string))

        val selection = GeneUtils.getBooleanSelection(obj)

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testBooleanSectionInteger() {

        val objInteger = ObjectGene("foo", listOf(IntegerGene("a", 1)))

        val selection = GeneUtils.getBooleanSelection(objInteger)

        val a = selection.fields.find { it.name == "a" } as BooleanGene


        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testBooleanSectionOptionalObject() {

        val objOpt = ObjectGene("foo", listOf(OptionalGene("Opti", StringGene("Opti", "hello"))))
        val selection = GeneUtils.getBooleanSelection(objOpt)

        val a = selection.fields.find { it.name == "Opti" } as BooleanGene
        //todo extra check
        a.value = true

        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{Opti}", rep)
    }

    @Test
    fun testBooleanSectionBoolean() {

        val objBoolean = ObjectGene("foo", listOf(BooleanGene("a")))

        val selection = GeneUtils.getBooleanSelection(objBoolean)

        val a = selection.fields.find { it.name == "a" } as BooleanGene


        val rep = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{a}", rep)
    }

    @Test
    fun testRepairBooleanSectionFF() {

        val objBoolean = ObjectGene("foo", listOf(BooleanGene("a", false), (BooleanGene("b", false))))

        GeneUtils.repairBooleanSelection(objBoolean)

        assertTrue(objBoolean.fields.any { it is BooleanGene && it.value == true })
    }

    @Test
    fun testRepairBooleanSectionOptionalTuple() {

        val booleanGene = BooleanGene("Boolean", value = false)

        val obj = ObjectGene(
            "object1", listOf(
                (OptionalGene(
                    "optional",
                    TupleGene(
                        "optionalTuple",
                        listOf(
                            IntegerGene("IntegerGene"),

                            ObjectGene("object2", listOf(booleanGene))// could never be an opt

                        ),
                        lastElementTreatedSpecially = true
                    )
                )),
                TupleGene("NonOptionalTuple", listOf(CycleObjectGene("cycle")), lastElementTreatedSpecially = true)
            )
        )
        assertFalse(booleanGene.value)
        GeneUtils.repairBooleanSelection(obj)
        assertTrue(booleanGene.value)
    }

    @Test
    fun testRepairBooleanSectionInTupleLastElementObject() {

        val objBooleanAndOptional = ObjectGene(
            "foo", listOf(
                BooleanGene("boolean",value = false),//to see if it is repaired
                OptionalGene("opt",
                    TupleGene(
                        "tuple1",
                        listOf(
                            ObjectGene(
                                "ObjInLastTuple",
                                listOf(BooleanGene("boolean"))
                            ),
                        ),
                        lastElementTreatedSpecially = true
                    ),isActive = false)//to see if it is repaired
            )
        )

        val tuple = TupleGene("tuple2", listOf(objBooleanAndOptional), lastElementTreatedSpecially = true)
        val rootObj = ObjectGene("rootObj", listOf(tuple))
        GeneUtils.repairBooleanSelection(rootObj)

        val objBoolAndOptRepaired=(rootObj.fields.first { it is TupleGene } as TupleGene).elements.last() as ObjectGene
        assertTrue(objBoolAndOptRepaired.fields.any { (it is BooleanGene && it.value) || (it is OptionalGene && it.isActive) })
    }

    @Test
    fun testRepairInPetclinic() {

        val actionCluster = mutableMapOf<String, Action>()
        val json = PetClinicCheckMain::class.java.getResource("/graphql/online/PetsClinic.json").readText()

        GraphQLActionBuilder.addActionsFromSchema(json, actionCluster)
        val pettypes = actionCluster.get("pettypes") as GraphQLAction
        assertTrue(pettypes.parameters[0] is GQReturnParam)
        assertTrue(pettypes.parameters[0].gene is ObjectGene)
        val objPetType = pettypes.parameters[0].gene as ObjectGene
        assertEquals(2, objPetType.fields.size)

         assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "id" })
         assertTrue(objPetType.fields.any { it is BooleanGene && it.name == "name" })


        (objPetType.fields[0] as BooleanGene).value = false
         (objPetType.fields[1] as BooleanGene).value = false
        assertFalse(objPetType.fields.any{ it is BooleanGene && it.value})
        GeneUtils.repairBooleanSelection(objPetType)
        assertTrue(objPetType.fields.any{ it is BooleanGene && it.value})

    }

    @Test
    fun testInterfaceSelectionName(){

        val a = OptionalGene("A",ObjectGene("A", listOf(BooleanGene("a1"))))
        val b = OptionalGene("B",ObjectGene("B", listOf(BooleanGene("b1"), BooleanGene("b2"))))

        val unionObj = ObjectGene("foo ${GqlConst.INTERFACE_TAG}", listOf(a,b))

        val res = unionObj.getValueAsPrintableString(listOf(), mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{...onA{a1}...onB{b1,b2}}", res)//with out the name foo
    }

    @Test
    fun testNestedInterfaceSelectionName(){

        val a = OptionalGene("A",ObjectGene("A", listOf(BooleanGene("a1"))))
        val b = OptionalGene("B",ObjectGene("B", listOf(BooleanGene("b1"), BooleanGene("b2"))))

        val optionalUnionObj = OptionalGene("foo ${GqlConst.INTERFACE_TAG}",ObjectGene("foo ${GqlConst.INTERFACE_TAG}", listOf(a,b)))

        val obj = ObjectGene("obj", listOf(optionalUnionObj))


        val res = obj.getValueAsPrintableString(listOf(), mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{foo{...onA{a1}...onB{b1,b2}}}", res)//with the name foo
    }


    @Test
    fun testCopyFields() {

        val obj = ObjectGene("Obj1",  listOf(StringGene("a", "hello"), StringGene("b", "hihi")))

        val objBase = ObjectGene("Obj1", listOf(StringGene("a", "hello")))

        val diff = obj.copyFields(objBase)

        assertEquals(1, diff.fields.size )
    }

    @Test
    fun testInterfaceSelection(){

        val a = OptionalGene("A",ObjectGene("A", listOf(BooleanGene("a1"))))
        val b = OptionalGene("B ${GqlConst.INTERFACE_BASE_TAG}",ObjectGene("B ${GqlConst.INTERFACE_BASE_TAG}", listOf(BooleanGene("b1"), BooleanGene("b2"))))

        val unionObj = ObjectGene("foo ${GqlConst.INTERFACE_TAG}", listOf(a,b))

        val res = unionObj.getValueAsPrintableString(listOf(), mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{...onA{a1}b1,b2}", res)//without the name foo and without "...on" for the object B
    }

    @Test
    fun testNestedInterfaceSelection(){

        val a = OptionalGene("A",ObjectGene("A", listOf(BooleanGene("a1"))))
        val b = OptionalGene("B ${GqlConst.INTERFACE_BASE_TAG}",ObjectGene("B${GqlConst.INTERFACE_BASE_TAG}", listOf(BooleanGene("b1"), BooleanGene("b2"))))

        val optionalUnionObj = OptionalGene("foo ${GqlConst.INTERFACE_TAG}",ObjectGene("foo ${GqlConst.INTERFACE_TAG}", listOf(a,b)))

        val obj = ObjectGene("obj", listOf(optionalUnionObj))


        val res = obj.getValueAsPrintableString(listOf(), mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
                .replace(" ", "") // remove empty space to make assertion less brittle

        assertEquals("{foo{...onA{a1}b1,b2}}", res)//with the name foo and without "...on" for the object B
    }

    @Test
    fun testGetBasicGeneBasedOnJavaType(){

        val intGene = GeneUtils.getBasicGeneBasedOnJavaType(java.lang.Integer::class.java, "foo")
        assertTrue(intGene is IntegerGene)

        val doubleGene = GeneUtils.getBasicGeneBasedOnJavaType(java.lang.Double::class.java, "foo")
        assertTrue(doubleGene is DoubleGene)

        val longGene = GeneUtils.getBasicGeneBasedOnJavaType(java.lang.Long::class.java, "foo")
        assertTrue(longGene is LongGene)

        val shortGene = GeneUtils.getBasicGeneBasedOnJavaType(java.lang.Short::class.java, "foo")
        assertTrue(shortGene is IntegerGene && shortGene.min != null && shortGene.max != null) //no ShortGene

        val numberGene = GeneUtils.getBasicGeneBasedOnJavaType(java.lang.Number::class.java, "foo")
        assertTrue(numberGene is IntegerGene)
    }
}