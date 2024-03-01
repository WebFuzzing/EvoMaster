package com.foo.graphql.db.exisitingdata

import com.foo.graphql.db.SpringWithDbController
import org.evomaster.client.java.sql.DbSpecification


class ExistingDataController : SpringWithDbController(ExistingDataApplication::class.java) {

    override fun schemaName() = "dbexisting.graphqls"


    override fun resetStateOfSUT() {
        super.resetStateOfSUT()

//        val rep = ctx!!.getBean<ExistingDataXRepository>(ExistingDataXRepository::class.java)
//        rep.save(ExistingDataX(42L, "Foo"))
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        val spec = super.getDbSpecifications()
        if (spec != null && spec.isNotEmpty())
            return mutableListOf(spec[0].withInitSqlScript("INSERT INTO EXISTING_DATAX (ID, NAME) VALUES (42, 'Foo')"))
        return spec
    }
}