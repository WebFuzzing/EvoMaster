package org.evomaster.core.mongo.filter

import org.bson.Document

class FilterDistanceEvaluator: FilterVisitor<Double,Document>() {

    override fun visit(comparisonFilter: ComparisonFilter<*>, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: AndFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: OrFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(allFilter: AllFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(elemMatchFilter: ElemMatchFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: SizeFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: InFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: NotInFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: NorFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: ExistsFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: NotExistsFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: RegexFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: SearchFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: WhereFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: ModFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: TypeFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: NotFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

}