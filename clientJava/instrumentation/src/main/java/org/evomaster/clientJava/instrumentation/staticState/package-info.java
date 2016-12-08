/**
 * This package should contain all and only the classes that do
 * have mutable static state.
 * In general, this should be avoided like the plague (much better
 * to use dependency injection like in JEE, Spring and Guice).
 * However, as this library code is instrumented in apriori unknown
 * SUTs, we cannot really use DI, as shouldn't add third-party
 * libraries unless if really necessary (and even in that case,
 * they should be shaded)... :(
 */
package org.evomaster.clientJava.instrumentation.staticState;