package org.evomaster.core.search.gene

abstract class IntegralNumberGene<T: Number> (
    name: String,
    value: T?,
    min: T?,
    max: T?,
    precision: Int?,
    minInclusive : Boolean,
    maxInclusive : Boolean,
) : NumberGene<T>(name, value, min, max, minInclusive, maxInclusive, precision, 0)