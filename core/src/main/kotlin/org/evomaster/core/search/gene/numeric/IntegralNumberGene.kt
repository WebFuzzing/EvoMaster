package org.evomaster.core.search.gene.numeric

/**
 * this class represent number which does have any fraction or decimals
 * such as integer, long, biginteger
 */
abstract class IntegralNumberGene<T: Number> (
    name: String,
    value: T?,
    min: T?,
    max: T?,
    precision: Int?,
    minInclusive : Boolean,
    maxInclusive : Boolean,
    multipleOf : T?
) : NumberGene<T>(name, value, min, max, minInclusive, maxInclusive, precision, 0, multipleOf)