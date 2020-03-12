package org.evomaster.core.mongo.filter

import org.bson.Document

class FilterParser {

    fun parse(filter: Document): Filter {
        return EqFilter<Integer>(fieldName ="age", value=Integer(32))
    }

}