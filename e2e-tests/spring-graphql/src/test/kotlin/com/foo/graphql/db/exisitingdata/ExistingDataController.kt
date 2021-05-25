package com.foo.graphql.db.exisitingdata

import com.foo.graphql.db.SpringWithDbController


class ExistingDataController : SpringWithDbController(ExistingDataApplication::class.java) {

    override fun schemaName() = "dbexisting.graphqls"


    override fun resetStateOfSUT() {
        super.resetStateOfSUT()

        val rep = ctx!!.getBean<ExistingDataXRepository>(ExistingDataXRepository::class.java)
        rep.save(ExistingDataX(42L, "Foo"))
    }

}