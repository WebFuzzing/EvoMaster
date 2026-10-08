package org.evomaster.client.java.controller.cassandra.model;

/**
 * Represents a parsed Cassandra {@code duration} literal, decomposed into its three
 * components: months, days, and nanoseconds.
 *
 * Instances are produced by
 * {@link org.evomaster.client.java.controller.cassandra.parser.CqlDurationLiteralParser}.
 */
public class CqlDurationLiteral {

    public final int  months;
    public final int  days;
    public final long nanos;

    public CqlDurationLiteral(int months, int days, long nanos) {
        this.months = months;
        this.days   = days;
        this.nanos  = nanos;
    }
}