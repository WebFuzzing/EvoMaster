package org.evomaster.client.java.instrumentation;

/**
 * Properties "-D" passed as input to JAR of SUTs when started with an External Controller
 */
public class InputProperties {

    /**
     * Specify to start a TCP server on the given port, listening to instructions
     * to collect coverage information
     */
    public static final String EXTERNAL_PORT_PROP = "evomaster.javaagent.external.port";


    /**
     * Option to write to disk the obtained coverage once the SUT ends
     */
    public static final String OUTPUT_FILE = "evomaster.javaagent.outputfile";


    /**
     * Which categories to apply for the method replacements.
     * This is a comma "," separated list of category names.
     */
    public static final String REPLACEMENT_CATEGORIES = "evomaster.javaagent.replacement_categories";
}
