package com.foo.graphql.db.exisitingdata

import com.foo.graphql.db.SpringWithDbController
import org.evomaster.client.java.controller.internal.db.DbSpecification


class ExistingDataController : SpringWithDbController(ExistingDataApplication::class.java) {

    override fun schemaName() = "dbexisting.graphqls"


    override fun resetStateOfSUT() {
        super.resetStateOfSUT()

//        val rep = ctx!!.getBean<ExistingDataXRepository>(ExistingDataXRepository::class.java)
//        rep.save(ExistingDataX(42L, "Foo"))
    }

    override fun getDbSpecification(): DbSpecification? {
        val spec = super.getDbSpecification();
        spec?.initSqlScript = "INSERT INTO EXISTING_DATAX (ID, NAME) VALUES (42, 'Foo')";
        return spec
    }
}