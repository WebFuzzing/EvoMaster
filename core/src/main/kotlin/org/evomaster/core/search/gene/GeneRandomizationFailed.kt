package org.evomaster.core.search.gene

/**
 * Exception thrown to indicate that a failure occurred during the process
 * of gene randomization. This exception typically signifies that an it was
 * not possible to generate a random valid gene value given the gene constraints.
 *
 * @param message The detail message providing more context about the specific failure.
 */
class GeneRandomizationFailed(message: String) : RuntimeException(message) {

}
