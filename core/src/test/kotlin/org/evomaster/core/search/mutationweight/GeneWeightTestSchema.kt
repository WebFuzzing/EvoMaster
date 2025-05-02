package org.evomaster.core.search.mutationweight

import io.swagger.parser.OpenAPIParser
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.EMConfig
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene

object GeneWeightTestSchema {

    private val config : EMConfig = EMConfig()
    private val actions: MutableMap<String, Action> = mutableMapOf()

    init {
        RestActionBuilderV3.addActionsFromSwagger(
            OpenAPIParser().readLocation("/swagger/artificial/gene_weight.json", null, null).openAPI,
            actions,
            enableConstraintHandling = config.enableSchemaConstraintHandling
        )
    }

    fun getActionFromCluster(name: String) = actions[name]

    /**
     * @return a rest individual which contains 4 genes
     * - three SQL genes, i.e., integerGene(w=1), DateGene(w=1), ObjectGene (w=2)
     * - one REST gene, i.e., ObjectGene(w=9)
     */
    fun newRestIndividual(name : String = "POST:/gw/foo", numSQLAction : Int = 1, numRestAction : Int = 1) : RestIndividual {
        val key =  Column("key", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            databaseType = DatabaseType.H2)
        val date =  Column("date", ColumnDataType.DATE, databaseType = DatabaseType.H2)
        val info = Column("info", ColumnDataType.JSON, databaseType = DatabaseType.H2)
        val table = Table("foo", setOf(key, date, info), setOf())

        val gk0 = SqlPrimaryKeyGene(key.name, table.name, IntegerGene(key.name, 1), 1)
        val gd0 = DateGene(date.name)
        val gj0 = ObjectGene(info.name, listOf(DateGene("field1"), IntegerGene("field2", 2)))
        val action0 =  SqlAction(table, setOf(key, date, info), 0L, listOf(gk0, gd0, gj0))

        val sqlActions = (0 until numSQLAction).map { action0.copy() as SqlAction }.toMutableList()

        val action1 = actions[name]?: throw IllegalArgumentException("$name cannot found in defined schema")

        // 1 dbaction with 3 genes, and 1 restaction with 1 bodyGene and 1 contentType
        return RestIndividual(actions = (0 until numRestAction).map { action1.copy() as RestCallAction }.toMutableList(), dbInitialization = sqlActions, sampleType = SampleType.RANDOM)
    }

}
